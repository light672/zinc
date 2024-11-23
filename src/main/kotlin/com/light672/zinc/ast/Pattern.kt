package com.light672.zinc.ast

internal sealed interface Pattern {
	class Identifier(
		val mut: Token?,
		val identifier: Token
	) : Pattern {
		override fun first() = mut ?: identifier
		override fun last() = identifier
	}

	class Tuple(
		val open: Token,
		val fields: List<Pattern>,
		val close: Token
	) : Pattern {
		override fun first() = open
		override fun last() = close
	}

	class Struct(
		val path: ComplexPath,
		val fields: List<Pair<Token, Pattern?>>,
		val rest: Token?,
		val close: Token
	) : Pattern {
		override fun first() = path.first()
		override fun last() = close
	}

	class TupleStruct(
		val path: ComplexPath,
		val fields: List<Pattern>,
		val close: Token
	) : Pattern {
		override fun first() = path.first()
		override fun last() = close
	}

	class Underscore(
		val token: Token
	) : Pattern {
		override fun first() = token
		override fun last() = token
	}

	class TypePath(
		val path: ComplexPath
	) : Pattern {
		override fun first() = path.first()
		override fun last() = path.last()
	}


	fun first(): Token
	fun last(): Token
}