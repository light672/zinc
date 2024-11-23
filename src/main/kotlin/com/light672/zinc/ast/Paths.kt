package com.light672.zinc.ast

internal sealed interface ComplexPath : Type {
	override fun first(): Token
	override fun last(): Token

	class Normal(
		val body: List<Segment>
	) : ComplexPath {
		override fun first() = body.first().token
		override fun last() = body.last().token
	}

	class Qualified(
		val head: QualifiedSegment,
		val body: List<Segment>
	) : ComplexPath {
		override fun first() = head.open
		override fun last() = body.lastOrNull()?.token ?: head.close
		class QualifiedSegment(
			val open: Token,
			val type: Type,
			val cast: ComplexPath,
			val close: Token
		)
	}

	class Segment(
		val token: Token
	)
}