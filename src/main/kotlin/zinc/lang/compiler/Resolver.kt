package zinc.lang.compiler

import zinc.Zinc
import zinc.builtin.ZincBoolean
import zinc.builtin.ZincChar
import zinc.builtin.ZincNumber
import zinc.builtin.ZincString
import zinc.lang.compiler.parsing.Expression
import zinc.lang.compiler.parsing.Statement
import zinc.lang.compiler.parsing.Token
import java.util.*

internal class Resolver(val runtime: Zinc.Runtime, val module: ZincModule) {
	private val locals = Stack<HashMap<String, Declaration>>()
	private val localTypes = Stack<HashMap<String, Type>>()

	private val currentVars get() = if (locals.empty()) module.globals else locals.peek()
	private val currentTypes get() = if (localTypes.empty()) module.types else localTypes.peek()
	private val inGlobal get() = locals.empty()

	private var funReturnType: Type? = null

	fun Statement.resolve(): Unit? {
		return when (this) {
			is Statement.VariableDeclaration -> resolve()
			is Statement.Function -> {
				resolve()?.resolveFunctionBlock() ?: return null
			}

			is Statement.ExpressionStatement -> expression.resolve()
		}
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
			is Expression.Grouping -> expression.resolve()
			is Expression.Return -> resolve()
			is Expression.Binary -> resolve()
			is Expression.GetVariable -> resolve()
			is Expression.SetVariable -> resolve()
			else -> {}
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

	fun Expression.Binary.resolve(): Unit? {
		left.resolve() ?: return null
		right.resolve() ?: return null
		if (left.getType() == Type.Number && right.getType() == Type.Number) return Unit
		runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Binary expression '${operator.lexeme}' can only be between 'num' and 'num'. Instead got '${left.getType()}' and '${right.getType()}'."
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
			is Expression.Binary -> Type.Number
			is Expression.Grouping -> expression.getType()
			is Expression.SetVariable -> value.getType()
			is Expression.GetVariable -> findVariable(variable)!!.type
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