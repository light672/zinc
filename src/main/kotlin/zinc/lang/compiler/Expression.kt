package zinc.lang.compiler

import zinc.builtin.ZincValue

abstract class Expression() {

	class Binary(val left: Expression, val right: Expression, val operator: Token) :
		Expression()

	class Unary(val right: Expression, val operator: Token) : Expression()
	class Literal(val literal: ZincValue?) : Expression()
	class Call(val callee: Expression) : Expression()
	class Variable(val name: Token) : Expression()
	class Assign(val variable: Variable, val value: Expression) : Expression()

	class IndexGet(val callee: Expression, val index: Expression) : Expression()
	class IndexSet(val callee: Expression, val index: Expression, val value: Expression) : Expression()

	class ObjectGet(val callee: Expression, val field: Token) : Expression()
	class ObjectSet(val callee: Expression, val field: Token, val value: Expression) : Expression()

	class Group(val expression: Expression) : Expression()
	class If(
		val condition: Expression, val thenBlock: List<Statement>, val elseBlock: List<Statement>?,
	) : Expression()
}
