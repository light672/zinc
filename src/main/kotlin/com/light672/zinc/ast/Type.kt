package com.light672.zinc.ast

internal sealed interface Type {
	class Tuple(
		val open: Token,
		val types: List<Type>,
		val close: Token
	) : Type {
		override fun first() = open
		override fun last() = close
	}

	fun first(): Token
	fun last(): Token
}