package zinc.lang.compiler

import zinc.builtin.ZincValue

abstract class Expression {

	interface Visitor {
		fun visit(expression: Binary)
		fun visit(expression: Literal)
		fun visit(expression: Grouping)
	}

	class Binary(val left: Expression, val right: Expression, val operator: Token) :
		Expression() {
		override fun accept(visitor: Visitor) = visitor.visit(this);
	}

	class Literal(val value: ZincValue) : Expression() {
		override fun accept(visitor: Visitor) = visitor.visit(this);
	}

	class Grouping(val expression: Expression) : Expression() {
		override fun accept(visitor: Visitor) = visitor.visit(this);
	}

	abstract fun accept(visitor: Visitor)
}
