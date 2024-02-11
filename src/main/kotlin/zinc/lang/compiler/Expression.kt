package zinc.lang.compiler

import zinc.builtin.ZincValue

abstract class Expression {

	interface Visitor<T> {
		fun visit(expression: Binary): T
		fun visit(expression: Literal): T
		fun visit(expression: Grouping): T
		fun visit(expression: GetVariable): T
	}

	class Binary(val left: Expression, val right: Expression, val operator: Token) :
		Expression() {
		override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this);
	}

	class Literal(val value: ZincValue) : Expression() {
		override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
	}

	class Grouping(val expression: Expression) : Expression() {
		override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this);
	}

	class GetVariable(val variable: Token) : Expression() {
		override fun <T> accept(visitor: Visitor<T>) = visitor.visit(this)
	}

	abstract fun <T> accept(visitor: Visitor<T>): T
}
