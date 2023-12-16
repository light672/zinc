package zinc.lang.compiler

import zinc.builtin.ZincValue

abstract class Expression() {
	abstract val type: Type

	class Binary(val left: Expression, val right: Expression, val operator: Token, override val type: Type) :
		Expression()

	class Unary(val right: Expression, val operator: Token, override val type: Type) : Expression()
	class Literal(val literal: ZincValue, override val type: Type) : Expression()
	class Call(val callee: Expression, override val type: Type) : Expression()
	class Variable(val name: Token, override val type: Type) : Expression()
	class Assign(val name: Token, val value: Expression) : Expression() {
		override val type = value.type
	}

	class IndexGet(val callee: Expression, val index: Expression, override val type: Type) : Expression()
	class IndexSet(val callee: Expression, val index: Expression, val value: Expression) : Expression() {
		override val type = value.type
	}

	class ObjectGet(val callee: Expression, val field: Token, override val type: Type) : Expression()
	class ObjectSet(val callee: Expression, val field: Token, val value: Expression) : Expression() {
		override val type = value.type
	}

	class Group(val expression: Expression, override val type: Type) : Expression()
	class If(
		val condition: Expression,
		val thenBlock: List<Statement>,
		val elseBlock: List<Statement>?,
		override val type: Type
	) : Expression()
}
