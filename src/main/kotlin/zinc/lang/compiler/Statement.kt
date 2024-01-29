package zinc.lang.compiler

abstract class Statement {
	interface Visitor {
		fun visit(expression: ExpressionStatement)
	}

	class ExpressionStatement(val expression: Expression) : Statement() {
		override fun accept(visitor: Visitor) = visitor.visit(this)
	}


	abstract fun accept(visitor: Visitor)
}