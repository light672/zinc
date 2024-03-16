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
			is Stmt.ExpressionStatement -> expression.resolve()
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
		if (initializer != null) initializer.resolve() ?: return null
		if (currentScope.parent == null) initializer ?: runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Global variables must have an initializer."
			)
		)
		val declaredType = if (type != null) getTypeFromName(type) ?: return null else null
		val initializerType = initializer?.getType()

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

	fun Expr.resolve(): Unit? {
		return when (this) {
			is Expr.Literal, is Expr.Unit -> {}
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

	fun Expr.Return.resolve(): Unit? {
		if (expression != null) expression.resolve() ?: return null
		val type = expression?.getType() ?: Type.Unit
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
		return Unit
	}

	fun Expr.Unary.resolve(): Unit? {
		right.resolve() ?: return null
		return when (operator.type) {
			Token.Type.BANG -> {
				if (right.getType() == Type.Bool) return Unit
				runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '!' on ${right.getType()}"))
				null
			}

			Token.Type.MINUS -> {
				if (right.getType() == Type.Number) return Unit
				runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '-' on ${right.getType()}"))
				null
			}

			else -> throw IllegalArgumentException("should not happen")
		}
	}

	fun Expr.Binary.resolve(): Unit? {
		left.resolve() ?: return null
		right.resolve() ?: return null
		if (left.getType() == Type.Number && right.getType() == Type.Number) return Unit
		runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Cannot perform binary '${operator.lexeme}' on '${left.getType()}' and '${right.getType()}'."
			)
		)
		return null
	}

	fun Expr.Logical.resolve(): Unit? {
		left.resolve() ?: return null
		right.resolve() ?: return null
		if (left.getType() == Type.Bool && right.getType() == Type.Bool) return Unit
		runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Cannot perform logical '${operator.lexeme}' on '${left.getType()}' and '${right.getType()}'."
			)
		)
		return null
	}

	fun Expr.GetVariable.resolve(): Unit? {
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
		return Unit
	}

	fun Expr.SetVariable.resolve(): Unit? {
		val variable = findVariable(variable) ?: return null
		value.resolve() ?: return null
		val expressionType = value.getType()
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
		return Unit
	}

	fun Expr.GetField.resolve(): Unit? {
		obj.resolve() ?: return null
		val type = obj.getType()
		if (type !is Type.Struct) {
			runtime.reportCompileError(CompilerError.OneRangeError(obj.getRange(), "Cannot get field using '.' on type '$type'."))
			return null
		}
		if (!type.fields.containsKey(field.lexeme)) {
			runtime.reportCompileError(CompilerError.TokenError(field, "Field '${field.lexeme}' does not exist in struct '$type'."))
			return null
		}
		return Unit
	}

	fun Expr.SetField.resolve(): Unit? {
		obj.resolve() ?: return null
		value.resolve()
		val type = obj.getType()
		if (type !is Type.Struct) {
			runtime.reportCompileError(CompilerError.OneRangeError(obj.getRange(), "Cannot get field using '.' on type '$type'."))
			return null
		}
		if (!type.fields.containsKey(field.lexeme)) {
			runtime.reportCompileError(CompilerError.TokenError(field, "Field '${field.lexeme}' does not exist in struct '$type'."))
			return null
		}
		val setType = value.getType()
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
		return Unit
	}

	fun Expr.Call.resolve(): Unit? {
		callee.resolve() ?: return null
		val calleeType = callee.getType()
		if (calleeType is Type.Function) {
			val argTypes = Array(calleeType.parameters.size) { i -> arguments[i].getType() }
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
			return Unit
		}
		runtime.reportCompileError(CompilerError.OneRangeError(callee.getRange(), "Can only call callable types, instead got '$calleeType'."))
		return null
	}

	fun Expr.InitializeStruct.resolve(): Unit? {
		val struct = findStruct(name) ?: return null
		val map = struct.type.fields.clone() as HashMap<String, Pair<IntRange, Expr>>
		for ((name, expression) in fields) {
			if (!struct.type.fields.containsKey(name.lexeme)) {
				runtime.reportCompileError(CompilerError.TokenError(name, "Struct '${struct}' does not have field '${name.lexeme}'."))
				return null
			}
			expression.resolve()
			if (expression.getType() != struct.type.fields[name.lexeme]!!.second) {
				runtime.reportCompileError(
					CompilerError.TwoRangeError(
						struct.type.fields[name.lexeme]!!.first, name.range.first..expression.getRange().last,
						"Declared with type '${struct.type.fields[name.lexeme]!!.second}'.",
						"Set with type '${expression.getType()}'.",
						"Value being set to '$struct::${name.lexeme}' has type '${expression.getType()}', while '$struct::${name.lexeme}' is type '${struct.type.fields[name.lexeme]!!.second}'."
					)
				)
				return null

			}
			map.remove(name.lexeme)
		}
		if (map.isEmpty()) return Unit
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

	fun Expr.getType(): Type {
		return when (this) {
			is Expr.Return -> Type.Nothing
			is Expr.Unit -> Type.Unit
			is Expr.Unary -> when (operator.type) {
				Token.Type.MINUS -> Type.Number
				Token.Type.BANG -> Type.Bool
				else -> throw IllegalArgumentException("Should not be possible")
			}

			is Expr.InitializeStruct -> findStruct(name)!!.type
			is Expr.Binary -> Type.Number
			is Expr.Logical -> Type.Bool
			is Expr.Grouping -> expression.getType()
			is Expr.SetVariable -> value.getType()
			is Expr.GetVariable -> findVariable(variable)!!.type
			is Expr.SetField -> value.getType()
			is Expr.GetField -> (obj.getType() as Type.Struct).fields[field.lexeme]!!.second
			is Expr.Call -> (callee.getType() as Type.Function).returnType
			is Expr.Literal -> {
				when (value) {
					is ZincNumber -> Type.Number
					is ZincBoolean -> Type.Bool
					is ZincChar -> Type.Char
					is ZincString -> Type.String
					else -> throw IllegalArgumentException("how did you even mess this up")
				}
			}
		}
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