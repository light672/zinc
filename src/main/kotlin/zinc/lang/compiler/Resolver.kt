package zinc.lang.compiler

import zinc.Zinc

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

	private fun Statement.VariableDeclaration.resolve() {
		val mutable = declaration.type == Token.Type.VAR
		val definedType = type?.lexeme?.let {
			currentScope.getTypeOrNull(it)?.apply {
				initializer ?: currentScope.declareVariable(type.lexeme, this, mutable)
			} ?: run {
				instance.reportCompileError("Type '$it' does not exist in the current scope.")
				return
			}
		}

		initializer?.run {
			initializer.resolve()
			val inferredType = initializer.type()
			definedType?.run {
				if (inferredType !== definedType) {
					instance.reportCompileError("Type '${type!!.lexeme}' does not match with initialized type of $type.")
					return
				}
			}
			currentScope.declareVariable(name.lexeme, inferredType, mutable)
			currentScope.defineVariable(name.lexeme)
		}
	}

	private fun Statement.Function.resolve() {
		val array = Array(arguments.size) {
			val type = currentScope.getTypeOrNull(arguments[it].second.lexeme) ?: run {
				instance.reportCompileError("Type '${arguments[it].second.lexeme}' does not exist in the current scope.")
				return
			}
			Pair(arguments[it].first.lexeme, type)
		}

		currentScope.defineAndDeclareFunction(name.lexeme, array, type?.let {
			currentScope.getTypeOrNull(type.lexeme) ?: run {
				instance.reportCompileError("Type '${it.lexeme}' does not exist in the current scope.")
				return
			}
		} ?: Type.Unit, instance)

		scope {
			for (pair in array) {
				currentScope.declareVariable(pair.first, pair.second, false)
			}
			for (stmt in body) stmt.resolve()
		}
	}

	private fun Expression.resolve() {
		when (this) {
			is Expression.Literal -> {}
			is Expression.Grouping -> expression.resolve()
			is Expression.Binary -> resolve()
			is Expression.GetVariable -> resolve()
		}
	}

	private fun Expression.Binary.resolve() {
		checkTypeSafety()
		left.resolve()
		right.resolve()
	}

	private fun Expression.GetVariable.resolve() {
		val variableInScope = currentScope.getVariableOrNull(variable.lexeme) ?: run {
			instance.reportCompileError("Variable '${variable.lexeme}' does not exist in the current scope.")
			return
		}
		if (!variableInScope.initialized)
			instance.reportCompileError("Cannot use variable '${variable.lexeme}' before it is initialized.")
	}

	private fun scope(block: () -> Unit) {
		currentScope = Scope(currentScope)
		block()
		currentScope = currentScope.parent!!
	}

	private fun Expression.type(): Type {

	}

}