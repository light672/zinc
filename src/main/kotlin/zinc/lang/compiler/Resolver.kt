package zinc.lang.compiler

import zinc.Zinc
import zinc.builtin.ZincBoolean
import zinc.builtin.ZincChar
import zinc.builtin.ZincNumber
import zinc.builtin.ZincString

internal class Resolver(val instance: Zinc.Runtime) {
	private val global = Zinc.defaultGlobalScope.copy()
	var currentScope = global
	fun resolve(ast: List<Statement>) {
		for (statement in ast) {
			statement.resolve()
		}
	}

	private fun Statement.resolve() {
		when (this) {
			is Statement.ExpressionStatement -> expression.resolve()
			is Statement.VariableDeclaration -> resolve()
			is Statement.Function -> resolve()
		}
	}

	private fun Statement.VariableDeclaration.resolve(): Unit? {
		val mutable = declaration.type == Token.Type.VAR
		val definedType = type?.lexeme?.let {
			currentScope.getTypeOrNull(it)?.apply {
				initializer ?: currentScope.declareVariable(type.lexeme, this, mutable)
			} ?: run {
				instance.reportCompileError("Type '$it' does not exist in the current scope.")
				return null
			}
		}

		initializer?.run {
			initializer.resolve() ?: return null
			val inferredType = initializer.type() ?: return null
			definedType?.run {
				if (inferredType !== definedType) {
					instance.reportCompileError("Type '${type!!.lexeme}' does not match with initialized type of $type.")
					return null
				}
			}
			currentScope.declareVariable(name.lexeme, inferredType, mutable)
			currentScope.defineVariable(name.lexeme)
		}
		return Unit
	}

	private fun Statement.Function.resolve(): Unit? {
		val array = Array(arguments.size) {
			val type = currentScope.getTypeOrNull(arguments[it].second.lexeme) ?: run {
				instance.reportCompileError("Type '${arguments[it].second.lexeme}' does not exist in the current scope.")
				return null
			}
			Pair(arguments[it].first.lexeme, type)
		}

		currentScope.defineAndDeclareFunction(name.lexeme, array, type?.let {
			currentScope.getTypeOrNull(type.lexeme) ?: run {
				instance.reportCompileError("Type '${it.lexeme}' does not exist in the current scope.")
				return null
			}
		} ?: Type.Unit, instance)

		scope {
			for (pair in array) {
				currentScope.declareVariable(pair.first, pair.second, false)
				currentScope.defineVariable(pair.first)
			}
			for (stmt in body) stmt.resolve()
		}
		return Unit
	}

	private fun Expression.resolve(): Unit? =
		when (this) {
			is Expression.Literal -> Unit
			is Expression.Grouping -> expression.resolve()
			is Expression.Binary -> resolve()
			is Expression.GetVariable -> resolve()
		}


	private fun Expression.Binary.resolve(): Unit? {
		left.resolve() ?: return null
		right.resolve() ?: return null
		val leftType = left.type()
		val rightType = right.type()

		if (leftType !== Type.Number || rightType !== Type.Number) {
			instance.reportCompileError("Invalid operands for '${operator.lexeme}', '${leftType}' and '${rightType}'.")
			return null
		}
		return Unit
	}

	private fun Expression.GetVariable.resolve(): Unit? {
		val variableInScope = currentScope.getVariableOrNull(variable.lexeme) ?: run {
			instance.reportCompileError("Variable '${variable.lexeme}' does not exist in the current scope.")
			return null
		}
		if (!variableInScope.initialized) {
			instance.reportCompileError("Cannot use variable '${variable.lexeme}' before it is initialized.")
			return null
		}
		return Unit
	}

	private fun Expression.type(): Type {
		return when (this) {
			is Expression.Grouping -> expression.type()
			is Expression.Literal -> type()
			is Expression.Binary -> type()
			is Expression.GetVariable -> type()
		}
	}

	private fun Expression.Literal.type() =
		when (value) {
			is ZincNumber -> Type.Number
			is ZincBoolean -> Type.Bool
			is ZincChar -> Type.Char
			is ZincString -> Type.String
			else -> throw IllegalArgumentException("There should be no other types in an Expression.Literal")
		}

	private fun Expression.Binary.type(): Type {
		if (left.type() === Type.Number) return Type.Number
		throw IllegalArgumentException("This should have been handled already")
	}

	private fun Expression.GetVariable.type() = currentScope.getVariableOrNull(variable.lexeme)!!.type

	private fun scope(block: () -> Unit) {
		currentScope = Scope(currentScope)
		block()
		currentScope = currentScope.parent!!
	}

}