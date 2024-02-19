package zinc.lang.compiler

import zinc.builtin.ZincValue

sealed class Expression {
	class Binary(val left: Expression, val right: Expression, val operator: Token) : Expression() {
		override fun getRange() = left.getRange().first..right.getRange().last
	}

	class Literal(val value: ZincValue, val token: Token) : Expression() {
		override fun getRange() = token.range
	}

	class Grouping(val expression: Expression, val beginning: Token, val end: Token) : Expression() {
		override fun getRange() = beginning.range.first..end.range.last
	}

	class GetVariable(val variable: Token) : Expression() {
		override fun getRange() = variable.range
	}

	class SetVariable(val variable: Token, val value: Expression) : Expression() {
		override fun getRange() = variable.range.first..value.getRange().last
	}

	class Unit(val beginning: Token, val end: Token) : Expression() {
		override fun getRange() = beginning.range.first..end.range.last
	}

	class Return(val token: Token, val expression: Expression?) : Expression() {
		override fun getRange() = token.range.first..(expression?.getRange()?.last ?: token.range.last)
	}


	abstract fun getRange(): IntRange
}
