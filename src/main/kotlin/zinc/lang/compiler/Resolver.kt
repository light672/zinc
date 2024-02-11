package zinc.lang.compiler

import zinc.Zinc

class Resolver(val instance: Zinc.Runtime) : Expression.Visitor<Unit>, Statement.Visitor {
	private val typeChecker = TypeChecker(instance)

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
		if (statement.initializer != null) {
			statement.initializer.resolve()
			if (statement.type != null) {
				val type = statement.initializer.checkTypeSafety() ?: return
				// TODO: compare the two types here
			}

		}
	}

	private fun Expression.resolve() = accept(this@Resolver)
	private fun Statement.resolve() = accept(this@Resolver)
	private fun Expression.checkTypeSafety() = accept(typeChecker)

}