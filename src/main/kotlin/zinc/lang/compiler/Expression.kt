package zinc.lang.compiler

import zinc.builtin.ZincValue

sealed class Expression {
	class Binary(val left: Expression, val right: Expression, val operator: Token) :
		Expression()

	class Literal(val value: ZincValue) : Expression()

	class Grouping(val expression: Expression) : Expression()

	class GetVariable(val variable: Token) : Expression()

	object Unit : Expression()

	class Return(val expression: Expression?) : Expression()
}
