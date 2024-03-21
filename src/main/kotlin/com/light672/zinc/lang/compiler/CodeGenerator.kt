package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.parsing.Expr
import com.light672.zinc.lang.compiler.parsing.Stmt
import com.light672.zinc.lang.compiler.parsing.Token
import com.light672.zinc.lang.runtime.opcodes.*

internal class CodeGenerator(val runtime: Zinc.Runtime, val module: ZincModule) {

	private var currentScope = module.globals

	val code = ArrayList<Byte>()
	val constants = ArrayList<ZincValue>()
	val ranges = ArrayList<IntRange>()

	fun Stmt.resolve(): Unit? {
		return when (this) {
			is Stmt.Struct -> throw IllegalArgumentException("Struct should never be resolved from this function.")
			is Stmt.Function -> throw IllegalArgumentException("Function should never be resolved from this function.")
			is Stmt.VariableDeclaration -> resolve()
			is Stmt.ExpressionStatement -> {
				val r = expression.resolve()?.let { Unit }
				code.add(OP_POP)
				r
			}
		}
	}

	fun Stmt.Struct.resolve(): Type.Struct? {
		if (currentScope.structs[name.lexeme] != null || currentScope.types[name.lexeme] != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentScope.structs[name.lexeme]!!.statement.getRange(), getRange(),
					"Type '${currentScope.structs[name.lexeme]!!.type.name}' first declared here.",
					"'${name.lexeme}' declared here again.",
					"Type '${name.lexeme}' declared with same name as other type in the same scope."
				)
			)
			return null
		}
		val type = Type.Struct(name.lexeme, HashMap())
		currentScope.structs[name.lexeme] = Struct(type, this)
		currentScope.types[name.lexeme] = type
		return type
	}

	fun Type.Struct.resolveStructInside(fieldsArray: Array<Pair<Token, Token>>): Unit? {
		var fieldTypeError = false
		val fieldTypes = Array(fieldsArray.size) { i ->
			val type = fieldsArray[i].second
			getTypeFromName(type) ?: run {
				fieldTypeError = true
				null
			}
		}

		if (fieldTypeError) return null
		for ((i, field) in fieldsArray.withIndex()) fields[field.first.lexeme] =
			Pair(field.first.range.first..field.second.range.last, fieldTypes[i]!!)
		return Unit
	}

	fun Stmt.VariableDeclaration.resolve(): Unit? {
		val mutable = declaration.type == Token.Type.VAR
		if (currentScope.parent == null) initializer ?: runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Global variables must have an initializer."
			)
		)
		val declaredType = if (type != null) getTypeFromName(type) ?: return null else null
		val initializerType = initializer?.let { it.resolve() ?: return null }

		if (declaredType != null && initializerType != null && declaredType != initializerType) {
			runtime.reportCompileError(
				CompilerError.OneRangeError(
					getRange(),
					"Declared type of '$declaredType' does not match initializer type of '$initializerType'."
				)
			)
			return null
		}

		val type = declaredType ?: initializerType!!
		val declaration = currentScope.variables[name.lexeme]?.first
		if (currentScope.parent == null && declaration != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					declaration.statement.getRange(), getRange(),
					"'${name.lexeme}' first declared here in top level scope.",
					"'${name.lexeme}' declared again in top level scope.",
					"'${name.lexeme} can only be declared once in top level scope."
				)
			)
			return null
		}

		return currentScope.addVariable(name.lexeme, Declaration(name.lexeme, type, mutable, this, initializer?.getRange(), false))
	}

	fun Declaration.resolveFunctionBlock() {
		scope((type as Type.Function).returnType) {
			statement as Stmt.Function
			for ((index, paramType) in type.parameters.withIndex()) {
				val pair = statement.arguments[index]
				val name = pair.first.lexeme
				val range = pair.first.range.first..pair.second.range.last
				val declaration = currentScope.variables[name]?.first
				if (declaration != null) {
					runtime.reportCompileError(
						CompilerError.TwoRangeError(
							declaration.initRange!!, range,
							"Function parameter '${declaration.name}' first declared here.",
							"'$name' declared here again in the same function.",
							"Function parameter '$name' declared twice in the function parameters."
						)
					)
					return
				}
				currentScope.addVariable(name, Declaration(name, paramType, false, statement, range, false))
			}
			for (stmt in statement.body) stmt.resolve()
			code.add(OP_RETURN)
		}
	}

	fun Stmt.Function.resolve(): Declaration? {
		val declaredType = if (type != null) getTypeFromName(type) else Type.Unit

		var paramTypeError = false
		val paramTypes = Array(arguments.size) { i ->
			val type = arguments[i].second
			getTypeFromName(type) ?: run {
				paramTypeError = true
				null
			}
		}

		if (paramTypeError) return null
		val params = Array(paramTypes.size) { i -> paramTypes[i]!! }
		val og = currentScope.variables[name.lexeme]?.first
		if (currentScope.parent == null && og != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					og.statement.getRange(), getRange(),
					"'${name.lexeme}' first declared here in top level scope.",
					"'${name.lexeme}' declared again in top level scope.",
					"'${name.lexeme}' can only be declared once in top level scope."
				)
			)
			return null
		}

		declaredType ?: return null

		val declaration = Declaration(name.lexeme, Type.Function(params, declaredType), false, this, getRange(), true)
		currentScope.addVariable(name.lexeme, declaration)
		code.add(OP_CREATE_FUNCTION)
		code.add(arguments.size.toByte())
		code.add(0)
		return declaration
	}

	fun Expr.resolve(): Type? {
		return when (this) {
			is Expr.Literal -> {
				when (value) {
					is ZincNumber -> {
						val number = value.value
						val int = number.toInt()
						if (int.toDouble() == number && int in 0..Short.MAX_VALUE) {
							val (a, b) = toBytes(int)
							code.add(OP_CREATE_NUM)
							code.add(a)
							code.add(b)
						} else {
							emitConstant(value)
						}
						Type.Number
					}

					is ZincBoolean -> {
						code.add(if (value.value) OP_TRUE else OP_FALSE)
						Type.Bool
					}

					is ZincChar -> {
						emitConstant(value)
						Type.Char
					}

					is ZincString -> {
						emitConstant(value)
						Type.String
					}

					else -> throw IllegalArgumentException("how did you even mess this up")
				}
			}

			is Expr.Unit -> Type.Unit
			is Expr.Grouping -> expression.resolve()
			is Expr.Return -> resolve()
			is Expr.Unary -> resolve()
			is Expr.Binary -> resolve()
			is Expr.GetVariable -> resolve()
			is Expr.SetVariable -> resolve()
			is Expr.GetField -> resolve()
			is Expr.SetField -> resolve()
			is Expr.Call -> resolve()
			is Expr.InitializeStruct -> resolve()
			is Expr.Logical -> resolve()
		}
	}

	fun Expr.Return.resolve(): Type? {
		val type = expression?.let { it.resolve() ?: return null } ?: Type.Unit
		currentScope.funReturnType ?: throw IllegalArgumentException("Somehow top level return got past parsing.")
		if (currentScope.funReturnType != type) {
			runtime.reportCompileError(
				CompilerError.OneRangeError(
					getRange(),
					"Return type '$type' does not match function return type of '${currentScope.funReturnType}'."
				)
			)
			return null
		}
		code.add(expression?.let { OP_RETURN_VALUE } ?: OP_RETURN)
		return Type.Nothing
	}

	fun Expr.Unary.resolve(): Type? {
		val type = right.resolve() ?: return null
		return when (operator.type) {
			Token.Type.BANG -> {
				if (type == Type.Bool) {
					code.add(OP_NOT)
					Type.Bool
				} else {
					runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '!' on $type"))
					null
				}
			}

			Token.Type.MINUS -> {
				if (type == Type.Number) {
					code.add(OP_NEG)
					Type.Number
				} else {
					runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '-' on $type"))
					null
				}
			}

			else -> throw IllegalArgumentException("should not happen")
		}
	}

	fun Expr.Binary.resolve(): Type? {
		val leftType = left.resolve() ?: return null
		val rightType = right.resolve() ?: return null
		if (leftType == Type.Number && rightType == Type.Number) {
			code.add(
				when (operator.type) {
					Token.Type.PLUS -> OP_ADD
					Token.Type.MINUS -> OP_SUB
					Token.Type.SLASH -> OP_DIV
					Token.Type.STAR -> OP_MUL
					Token.Type.CARET -> OP_POW
					else -> throw IllegalArgumentException()
				}
			)
			return Type.Number
		}
		runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Cannot perform binary '${operator.lexeme}' on '$leftType' and '$rightType'."
			)
		)
		return null
	}

	fun Expr.Logical.resolve(): Type? {
		val leftType = left.resolve() ?: return null
		val rightType: Type
		when (operator.type) {
			Token.Type.AND -> {
				val endJump = emitJump(OP_JIF)
				code.add(OP_POP)
				rightType = right.resolve() ?: return null
				patchJump(endJump, getRange())
			}

			Token.Type.OR -> {
				val endJump = emitJump(OP_JIT)
				code.add(OP_POP)
				rightType = right.resolve() ?: return null
				patchJump(endJump, getRange())
			}

			else -> throw IllegalArgumentException()
		}
		if (leftType == Type.Bool && rightType == Type.Bool) return Type.Bool
		runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Cannot perform logical '${operator.lexeme}' on '$leftType' and '$rightType'."
			)
		)
		return null
	}

	fun Expr.GetVariable.resolve(): Type? {
		val (variable, index) = findVariable(variable) ?: return null
		if (variable.initRange == null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					variable.statement.getRange(), getRange(),
					"Variable declared here without an initializer.",
					"Variable used before being initialized.",
					"Variable '${variable.name}' used before being initialized."
				)
			)
			return null
		}
		code.add(OP_GET_STACK)
		code.add(index.toByte())
		return variable.type
	}

	fun Expr.SetVariable.resolve(): Type? {
		val (variable, index) = findVariable(variable) ?: return null
		val expressionType = value.resolve() ?: return null
		if (variable.type != expressionType) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					variable.statement.getRange(), getRange(),
					"Declared with type '${variable.type}'.",
					"Set with type '$expressionType'.",
					"Value being set to '${variable.name}' has type '$expressionType', while '${variable.name}' is type '${variable.type}'."
				)
			)
			return null
		}
		if (variable.initRange == null) variable.initRange = getRange()
		code.add(OP_SET_STACK)
		code.add(index.toByte())
		return expressionType
	}

	fun Expr.GetField.resolve(): Type? {
		val type = obj.resolve() ?: return null
		if (type !is Type.Struct) {
			runtime.reportCompileError(CompilerError.OneRangeError(obj.getRange(), "Cannot get field using '.' on type '$type'."))
			return null
		}
		if (!type.fields.containsKey(field.lexeme)) {
			runtime.reportCompileError(CompilerError.TokenError(field, "Field '${field.lexeme}' does not exist in struct '$type'."))
			return null
		}
		code.add(OP_GET_IND)
		code.add(type.fields.keys.indexOf(field.lexeme).toByte())
		return type.fields[field.lexeme]!!.second
	}

	fun Expr.SetField.resolve(): Type? {
		val type = obj.resolve() ?: return null
		val setType = value.resolve()
		if (type !is Type.Struct) {
			runtime.reportCompileError(CompilerError.OneRangeError(obj.getRange(), "Cannot get field using '.' on type '$type'."))
			return null
		}
		if (!type.fields.containsKey(field.lexeme)) {
			runtime.reportCompileError(CompilerError.TokenError(field, "Field '${field.lexeme}' does not exist in struct '$type'."))
			return null
		}
		val declaredType = type.fields[field.lexeme]!!.second
		if (setType != declaredType) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					type.fields[field.lexeme]!!.first, getRange(),
					"Declared with type '$declaredType'.",
					"Set with type '$setType'.",
					"Value being set to '$type::${field.lexeme}' has type '$setType', while '$type::${field.lexeme}' is type '$declaredType'."
				)
			)
			return null
		}
		code.add(OP_SET_IND)
		code.add(type.fields.keys.indexOf(field.lexeme).toByte())
		return setType
	}

	fun Expr.Call.resolve(): Type? {
		val calleeType = callee.resolve() ?: return null
		if (calleeType is Type.Function) {
			val argTypes = Array(calleeType.parameters.size) { i -> arguments[i].resolve() ?: return null }
			if (!argTypes.contentEquals(calleeType.parameters)) {
				runtime.reportCompileError(
					CompilerError.OneRangeError(
						callee.getRange(),
						"Function expected argument types '${Type.Function.typeArrayToString(calleeType.parameters)}', but got ${
							Type.Function.typeArrayToString(
								argTypes
							)
						}'."
					)
				)
				return null
			}
			code.add(OP_CALL)
			code.add(argTypes.size.toByte())
			return calleeType.returnType
		}
		runtime.reportCompileError(CompilerError.OneRangeError(callee.getRange(), "Can only call callable types, instead got '$calleeType'."))
		return null
	}

	fun Expr.InitializeStruct.resolve(): Type? {
		val struct = findStruct(name) ?: return null
		val map = struct.type.fields.clone() as HashMap<String, Pair<IntRange, Expr>>
		for ((name, expression) in fields) {
			if (!struct.type.fields.containsKey(name.lexeme)) {
				runtime.reportCompileError(CompilerError.TokenError(name, "Struct '${struct}' does not have field '${name.lexeme}'."))
				return null
			}
			val exprType = expression.resolve() ?: return null
			if (exprType != struct.type.fields[name.lexeme]!!.second) {
				runtime.reportCompileError(
					CompilerError.TwoRangeError(
						struct.type.fields[name.lexeme]!!.first, name.range.first..expression.getRange().last,
						"Declared with type '${struct.type.fields[name.lexeme]!!.second}'.",
						"Set with type '$exprType'.",
						"Value being set to '$struct::${name.lexeme}' has type '$exprType', while '$struct::${name.lexeme}' is type '${struct.type.fields[name.lexeme]!!.second}'."
					)
				)
				return null

			}
			map.remove(name.lexeme)
		}
		if (map.isEmpty()) {
			code.add(OP_ALLOC)
			code.add(struct.type.fields.size.toByte())
			return struct.type
		}
		runtime.reportCompileError(
			CompilerError.OneRangeError(getRange(), "Struct declaration is missing fields ${
				run {
					val builder = StringBuilder()
					for (name in map.keys) {
						builder.append("'$name', ")
					}
					if (builder[builder.length - 2] == ',') {
						builder.delete(builder.length - 2, builder.length)
					}
					builder.toString()
				}
			}.")
		)
		return null
	}

	fun emitConstant(value: ZincValue) {
		constants.add(value)
		val (a, b) = toBytes(constants.size - 1)
		code.add(OP_CONST)
		code.add(a)
		code.add(b)
	}

	fun emitJump(instruction: Byte): Int {
		code.add(instruction)
		code.add(0xFF.toByte())
		code.add(0xFF.toByte())
		return code.size - 2
	}

	fun patchJump(offset: Int, range: IntRange): Unit? {
		val jump = code.size - offset - 2
		if (jump > Short.MAX_VALUE) {
			runtime.reportCompileError(CompilerError.OneRangeError(range, "Too much code to jump over."))
			return null
		}
		val (a, b) = toBytes(jump)
		code[offset] = a
		code[offset + 1] = b
		return Unit
	}


	fun findVariable(name: Token): Pair<Declaration, Int>? {
		var scope: Scope? = currentScope
		while (scope != null) {
			if (scope.variables[name.lexeme] != null) return scope.variables[name.lexeme]
			scope = scope.parent
		}
		runtime.reportCompileError(CompilerError.TokenError(name, "Variable '${name.lexeme}' does not exist in the current scope."))
		return null
	}

	fun findStruct(name: Token): Struct? {
		var scope: Scope? = currentScope
		while (scope != null) {
			if (scope.structs[name.lexeme] != null) return scope.structs[name.lexeme]
			scope = scope.parent
		}
		runtime.reportCompileError(
			CompilerError.TokenError(
				name,
				"Struct or variant '${name.lexeme}' does not exist in the current scope."
			)
		)
		return null
	}


	fun getTypeFromName(name: Token): Type? {
		var scope: Scope? = currentScope
		while (scope != null) {
			if (scope.types[name.lexeme] != null) return scope.types[name.lexeme]
			scope = scope.parent
		}
		runtime.reportCompileError(CompilerError.TokenError(name, "Type '${name.lexeme}' does not exist in the current scope."))
		return null
	}


	private inline fun scope(functionType: Type? = currentScope.funReturnType, block: () -> Unit) {
		currentScope = Scope(currentScope, functionType)
		block()
		currentScope = currentScope.parent!!
	}
}