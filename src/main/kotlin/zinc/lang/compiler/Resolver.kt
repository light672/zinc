package zinc.lang.compiler

import zinc.Zinc

internal class Resolver(val instance: Zinc.Runtime) {
	private val typeChecker = TypeChecker(instance, this)
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
		if (type != null) {
			if (!currentScope.hasType(type.lexeme)) {
				instance.reportCompileError("Type '${type.lexeme}' does not exist in the current scope.")
				return
			}
			if (initializer == null) currentScope.declareVariable(
				name.lexeme,
				currentScope.getType(statement.type.lexeme)
			)
		}
		if (initializer != null) {
			initializer.resolve()
			val type = initializer.checkTypeSafety() ?: return
			if (type != null) {
				if (type !== currentScope.getType(type.lexeme)) {
					instance.reportCompileError("Type '${type.lexeme}' does not match with initialized type of '$type'.")
					return
				}
			}
			currentScope.declareAndDefineVariable(name.lexeme, type)
		}
	}

	private fun Statement.Function.resolve() {
		val array = ArrayList<Pair<String, Type>>().also {
			for (param in arguments) {
				val type =
					if (currentScope.hasType(param.second.lexeme))
						currentScope.getType(param.second.lexeme)
					else {
						instance.reportCompileError("Type '${param.second.lexeme}' does not exist in the current scope.")
						return
					}
				it.add(Pair(param.first.lexeme, type))
			}
		}.toTypedArray()

		currentScope.defineAndDeclareFunction(
			name.lexeme,
			array,
			if (type == null)
				Type.Unit
			else if (currentScope.hasType(type.lexeme))
				currentScope.getType(type.lexeme)
			else {
				instance.reportCompileError("Type '${type.lexeme}' does not exist in the current scope.")
				return
			},
			instance
		)
		scope {
			for (pair in array) currentScope.declareAndDefineVariable(pair.first, pair.second)
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
		if (!currentScope.hasVariable(variable.lexeme)) {
			instance.reportCompileError("Variable '${variable.lexeme}' does not exist in the current scope.")
			return
		}
		val gottenVariable = currentScope.getVariable(variable.lexeme)
		if (!gottenVariable.initialized) {
			instance.reportCompileError("Cannot use variable '${variable.lexeme}' before it is initialized.")
		}
	}

	private fun scope(block: () -> Unit) {
		currentScope = Scope(currentScope)
		block()
		currentScope = currentScope.parent!!
	}

}