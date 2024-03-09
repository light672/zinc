package com.light672.zinc.lang.compiler

import com.light672.zinc.builtin.ZincBoolean
import com.light672.zinc.builtin.ZincChar
import com.light672.zinc.builtin.ZincNumber
import com.light672.zinc.builtin.ZincString
import com.light672.zinc.lang.compiler.parsing.Expression
import com.light672.zinc.lang.compiler.parsing.Statement
import com.light672.zinc.lang.compiler.parsing.Token
import java.util.*

internal class Resolver(val runtime: com.light672.zinc.Zinc.Runtime, val module: ZincModule) {
	private val locals = Stack<HashMap<String, Declaration>>()
	private val localTypes = Stack<HashMap<String, Type>>()

	private val currentVars get() = if (locals.empty()) module.globals else locals.peek()
	private val currentTypes get() = if (localTypes.empty()) module.types else localTypes.peek()
	private val inGlobal get() = locals.empty()

	private var funReturnType: Type? = null

	fun Statement.resolve(): Unit? {
		return when (this) {
			is Statement.Struct -> throw IllegalArgumentException("Struct should never be resolved from this function.")
			is Statement.Function -> throw IllegalArgumentException("Function should never be resolved from this function.")
			is Statement.VariableDeclaration -> resolve()
			is Statement.ExpressionStatement -> expression.resolve()
		}
	}

	fun Statement.Struct.resolve(): Type.Struct? {
		if (currentTypes[name.lexeme] != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentVars[name.lexeme]!!.statement.getRange(), getRange(),
					"Struct '${currentVars[name.lexeme]!!.name}' first declared here.",
					"'${name.lexeme}' declared here again.",
					"Struct '${name.lexeme}' declared with same name as other function in the same scope."
				)
			)
			return null
		}
		val type = Type.Struct(name.lexeme, HashMap<String, Pair<IntRange, Type>>())
		currentTypes[name.lexeme] = type
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

	fun Statement.VariableDeclaration.resolve(): Unit? {
		val mutable = declaration.type == Token.Type.VAR
		if (initializer != null) initializer.resolve() ?: return null
		if (inGlobal) initializer ?: runtime.reportCompileError(CompilerError.OneRangeError(getRange(), "Global variables must have an initializer."))
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
		if (currentVars[name.lexeme]?.function == true) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentVars[name.lexeme]!!.statement.getRange(), getRange(),
					"Function '${currentVars[name.lexeme]!!.name}' first declared here.",
					"'${name.lexeme}' declared here again.",
					"Variable '${name.lexeme}' declared with same name as function in the same scope."
				)
			)
			return null
		}
		if (inGlobal && currentVars[name.lexeme] != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentVars[name.lexeme]!!.statement.getRange(), getRange(),
					"'${name.lexeme}' first declared here in top level scope.",
					"'${name.lexeme}' declared again in top level scope.",
					"'${name.lexeme} can only be declared once in top level scope."
				)
			)
			return null
		}

		currentVars[name.lexeme] = Declaration(name.lexeme, type, mutable, this, initializer?.getRange(), false)
		return Unit
	}

	fun Declaration.resolveFunctionBlock() {
		scope((type as Type.Function).returnType) {
			statement as Statement.Function
			for ((index, paramType) in type.parameters.withIndex()) {
				val pair = statement.arguments[index]
				val name = pair.first.lexeme
				val range = pair.first.range.first..pair.second.range.last
				if (currentVars[name] != null) {
					val current = currentVars[name]!!
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
				currentVars[name] = Declaration(name, paramType, false, statement, range, false)
			}
			for (stmt in statement.body) stmt.resolve()
		}
	}

	fun Statement.Function.resolve(): Declaration? {
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
		if (currentVars[name.lexeme]?.function == true) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentVars[name.lexeme]!!.statement.getRange(), getRange(),
					"Function '${currentVars[name.lexeme]!!.name}' first declared here.",
					"'${name.lexeme}' declared here again.",
					"Function '${name.lexeme}' declared with same name as other function in the same scope."
				)
			)
			return null
		}
		if (inGlobal && currentVars[name.lexeme] != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					currentVars[name.lexeme]!!.statement.getRange(), getRange(),
					"'${name.lexeme}' first declared here in top level scope.",
					"'${name.lexeme}' declared again in top level scope.",
					"'${name.lexeme} can only be declared once in top level scope."
				)
			)
			return null
		}

		declaredType ?: return null

		val declaration = Declaration(name.lexeme, Type.Function(params, declaredType), false, this, getRange(), true)
		currentVars[name.lexeme] = declaration
		return declaration
	}

	fun Expression.resolve(): Unit? {
		return when (this) {
			is Expression.Literal, is Expression.Unit -> {}
			is Expression.Grouping -> expression.resolve()
			is Expression.Return -> resolve()
			is Expression.Unary -> resolve()
			is Expression.Binary -> resolve()
			is Expression.GetVariable -> resolve()
			is Expression.SetVariable -> resolve()
			is Expression.GetField -> resolve()
			is Expression.SetField -> resolve()
			is Expression.Call -> resolve()
			is Expression.InitializeStruct -> resolve()
		}
	}

	fun Expression.Return.resolve(): Unit? {
		if (expression != null) expression.resolve() ?: return null
		val type = expression?.getType() ?: Type.Unit
		funReturnType ?: throw IllegalArgumentException("Somehow top level return got past parsing.")
		if (funReturnType != type) {
			runtime.reportCompileError(
				CompilerError.OneRangeError(
					getRange(),
					"Return type '$type' does not match function return type of '$funReturnType'."
				)
			)
			return null
		}
		return Unit
	}

	fun Expression.Unary.resolve(): Unit? {
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

	fun Expression.Binary.resolve(): Unit? {
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

	fun Expression.GetVariable.resolve(): Unit? {
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

	fun Expression.SetVariable.resolve(): Unit? {
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

	fun Expression.GetField.resolve(): Unit? {
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

	fun Expression.SetField.resolve(): Unit? {
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

	fun Expression.Call.resolve(): Unit? {
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

	fun Expression.InitializeStruct.resolve(): Unit? {
		val type = getTypeFromName(name) ?: return null
		if (type !is Type.Struct) {
			runtime.reportCompileError(
				CompilerError.TokenError(
					name,
					"Struct or variant '${name.lexeme}' does not exist in the current scope."
				)
			)
			return null
		}
		val map = type.fields.clone() as HashMap<String, Pair<IntRange, Expression>>
		for ((name, expression) in fields) {
			if (!type.fields.containsKey(name.lexeme)) {
				runtime.reportCompileError(CompilerError.TokenError(name, "Struct '${type}' does not have field '${name.lexeme}'."))
				return null
			}
			expression.resolve()
			if (expression.getType() != type.fields[name.lexeme]!!.second) {
				runtime.reportCompileError(
					CompilerError.TwoRangeError(
						type.fields[name.lexeme]!!.first, name.range.first..expression.getRange().last,
						"Declared with type '${type.fields[name.lexeme]!!.second}'.",
						"Set with type '${expression.getType()}'.",
						"Value being set to '$type::${name.lexeme}' has type '${expression.getType()}', while '$type::${name.lexeme}' is type '${type.fields[name.lexeme]!!.second}'."
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
		for (map in locals) if (map[name.lexeme] != null) return map[name.lexeme]
		if (module.globals[name.lexeme] != null) return module.globals[name.lexeme]
		runtime.reportCompileError(CompilerError.TokenError(name, "Variable '${name.lexeme}' does not exist in the current scope."))
		return null
	}

	fun Expression.getType(): Type {
		return when (this) {
			is Expression.Return -> Type.Nothing
			is Expression.Unit -> Type.Unit
			is Expression.Unary -> when (operator.type) {
				Token.Type.MINUS -> Type.Number
				Token.Type.BANG -> Type.Bool
				else -> throw IllegalArgumentException("Should not be possible")
			}

			is Expression.InitializeStruct -> getTypeFromName(name)!!
			is Expression.Binary -> Type.Number
			is Expression.Grouping -> expression.getType()
			is Expression.SetVariable -> value.getType()
			is Expression.GetVariable -> findVariable(variable)!!.type
			is Expression.SetField -> value.getType()
			is Expression.GetField -> (obj.getType() as Type.Struct).fields[field.lexeme]!!.second
			is Expression.Call -> (callee.getType() as Type.Function).returnType
			is Expression.Literal -> {
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
		for (map in localTypes) if (map[name.lexeme] != null) return map[name.lexeme]
		if (module.types[name.lexeme] != null) return module.types[name.lexeme]
		runtime.reportCompileError(CompilerError.TokenError(name, "Type '${name.lexeme}' does not exist in the current scope."))
		return null
	}

	private inline fun scope(functionType: Type? = funReturnType, block: () -> Unit) {
		val prevReturnType = funReturnType
		funReturnType = functionType
		locals.push(HashMap())
		localTypes.push(HashMap())
		block()
		locals.pop()
		localTypes.pop()
		funReturnType = prevReturnType
	}
}