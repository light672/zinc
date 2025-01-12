package com.light672.zinc.ast

internal sealed interface Pattern {
	data class Identifier(val token: Token) : Pattern
	data class Wildcard(val token: Token) : Pattern
	data class Literal(val token: Token) : Pattern
	data class Tuple(val start: Token, val fields: List<Pattern>, val end: Token) : Pattern

	fun range() = when (this) {
		is Identifier -> token.asRange()
		is Literal -> token.asRange()
		is Wildcard -> token.asRange()
		is Tuple -> start..end
	}
}