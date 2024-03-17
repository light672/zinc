package com.light672.zinc.lang.compiler

import com.light672.zinc.builtin.ZincBoolean
import com.light672.zinc.builtin.ZincChar
import com.light672.zinc.builtin.ZincNumber
import com.light672.zinc.builtin.ZincString
import com.light672.zinc.lang.compiler.parsing.Expr
import com.light672.zinc.lang.compiler.parsing.Stmt
import com.light672.zinc.lang.compiler.parsing.Token

internal class Resolver(val runtime: com.light672.zinc.Zinc.Runtime, val module: ZincModule) {

	private var currentScope = module.globals

	fun Stmt.resolve(): Unit? {
		return when (this) {
			is Stmt.Struct -> throw IllegalArgumentException("Struct should never be resolved from this function.")
			is Stmt.Function -> throw IllegalArgumentException("Function should never be resolved from this function.")
			is Stmt.VariableDeclaration -> resolve()
			is Stmt.ExpressionStatement -> expression.resolve()?.let { Unit }
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
		if (currentScope.parent == null && currentScope.variables[name.lexeme] != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentScope.variables[name.lexeme]!!.statement.getRange(), getRange(),
					"'${name.lexeme}' first declared here in top level scope.",
					"'${name.lexeme}' declared again in top level scope.",
					"'${name.lexeme} can only be declared once in top level scope."
				)
			)
			return null
		}

		currentScope.variables[name.lexeme] = Declaration(name.lexeme, type, mutable, this, initializer?.getRange(), false)
		return Unit
	}

	fun Declaration.resolveFunctionBlock() {
		scope((type as Type.Function).returnType) {
			statement as Stmt.Function
			for ((index, paramType) in type.parameters.withIndex()) {
				val pair = statement.arguments[index]
				val name = pair.first.lexeme
				val range = pair.first.range.first..pair.second.range.last
				if (currentScope.variables[name] != null) {
					val current = currentScope.variables[name]!!
					runtime.reportCompileError(
						CompilerError.TwoRangeError(
							current.initRange!!, range,
							"Function parameter '${current.name}' first declared here.",
							"'$name' declared here again in the same function.",
							"Function parameter '$name' declared twice in the function parameters."
						)
					)
					return
				}
				currentScope.variables[name] = Declaration(name, paramType, false, statement, range, false)
			}
			for (stmt in statement.body) stmt.resolve()
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
		if (currentScope.parent == null && currentScope.variables[name.lexeme] != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentScope.variables[name.lexeme]!!.statement.getRange(), getRange(),
					"'${name.lexeme}' first declared here in top level scope.",
					"'${name.lexeme}' declared again in top level scope.",
					"'${name.lexeme} can only be declared once in top level scope."
				)
			)
			return null
		}

		declaredType ?: return null

		val declaration = Declaration(name.lexeme, Type.Function(params, declaredType), false, this, getRange(), true)
		currentScope.variables[name.lexeme] = declaration
		return declaration
	}

	fun Expr.resolve(): Type? {
		return when (this) {
			is Expr.Literal -> {
				when (value) {
					is ZincNumber -> Type.Number
					is ZincBoolean -> Type.Bool
					is ZincChar -> Type.Char
					is ZincString -> Type.String
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
		return Type.Nothing
	}

	fun Expr.Unary.resolve(): Type? {
		val type = right.resolve() ?: return null
		return when (operator.type) {
			Token.Type.BANG -> {
				if (type == Type.Bool) return Type.Bool
				runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '!' on $type"))
				null
			}

			Token.Type.MINUS -> {
				if (type == Type.Number) return Type.Number
				runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '-' on $type"))
				null
			}

			else -> throw IllegalArgumentException("should not happen")
		}
	}

	fun Expr.Binary.resolve(): Type? {
		val leftType = left.resolve() ?: return null
		val rightType = right.resolve() ?: return null
		if (leftType == Type.Number && rightType == Type.Number) return Type.Number
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
		val rightType = right.resolve() ?: return null
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
		val variable = findVariable(variable) ?: return null
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
		return variable.type
	}

	fun Expr.SetVariable.resolve(): Type? {
		val variable = findVariable(variable) ?: return null
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
		if (map.isEmpty()) return struct.type
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


	fun findVariable(name: Token): Declaration? {
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