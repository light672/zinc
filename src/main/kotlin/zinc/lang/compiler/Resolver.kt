package zinc.lang.compiler

class Resolver : Expression.Visitor, Statement.Visitor {
	override fun visit(expression: Expression.Binary) {
		TODO("Not yet implemented")
	}

	override fun visit(expression: Expression.Literal) {
		TODO("Not yet implemented")
	}

	override fun visit(expression: Expression.Grouping) {
		TODO("Not yet implemented")
	}

	override fun visit(expression: Statement.ExpressionStatement) {
		TODO("Not yet implemented")
	}

	override fun visit(expression: Statement.Function) {
		TODO("Not yet implemented")
	}

	override fun visit(expression: Statement.VariableDeclaration) {
		TODO("Not yet implemented")
	}
}