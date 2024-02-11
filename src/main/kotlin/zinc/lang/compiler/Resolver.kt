package zinc.lang.compiler

import zinc.Zinc

class Resolver(val instance: Zinc.Runtime) : Expression.Visitor<Unit>, Statement.Visitor {
	private val typeChecker = TypeChecker(instance)
	private val global = Zinc.defaultGlobalScope.copy()
	private var currentScope = global
	fun resolve(ast: List<Statement>) {
		for (statement in ast) {
			statement.resolve()
		}
	}

	override fun visit(expression: Expression.Binary) {
		expression.checkTypeSafety()
		expression.left.resolve()
		expression.right.resolve()
	}

	override fun visit(expression: Expression.Literal) {}

	override fun visit(expression: Expression.Grouping) {
		expression.expression.resolve()
	}

	override fun visit(statement: Statement.ExpressionStatement) {
		statement.expression.resolve()
	}

	override fun visit(statement: Statement.Function) {
		TODO("Not yet implemented")
	}

	override fun visit(statement: Statement.VariableDeclaration) {
		if (statement.type != null) {
			if (!currentScope.hasType(statement.type.lexeme)) {
				instance.reportCompileError("Type '${statement.type.lexeme}' does not exist in the current scope.")
				return
			}
			if (statement.initializer == null) currentScope.declareVariable(
				statement.name.lexeme,
				currentScope.getType(statement.type.lexeme)
			)
		}
		if (statement.initializer != null) {
			statement.initializer.resolve()
			val type = statement.initializer.checkTypeSafety() ?: return
			if (statement.type != null) {
				if (type !== currentScope.getType(statement.type.lexeme)) {
					instance.reportCompileError("Type '${statement.type.lexeme}' does not match with initialized type of '$type'.")
					return
				}
			}
			currentScope.declareAndDefineVariable(statement.name.lexeme, type)
		}
	}

	private fun scope(block: () -> Unit) {
		currentScope = Scope(currentScope)
		block()
		currentScope = currentScope.parent!!
	}

	private fun Expression.resolve() = accept(this@Resolver)
	private fun Statement.resolve() = accept(this@Resolver)
	private fun Expression.checkTypeSafety() = accept(typeChecker)

}