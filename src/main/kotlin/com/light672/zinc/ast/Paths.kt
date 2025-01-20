package com.light672.zinc.ast


internal data class ComplexSegment(val id: Token, val generics: GenericArgs?) {
	fun range() = id..(generics?.end ?: id)
}

internal sealed interface ComplexPath {
	data class Normal(val body: List<ComplexSegment>) : ComplexPath
	data class Qualified(val start: Token, val type: Type, val trait: Normal?, val end: Token, val body: List<ComplexSegment>) : ComplexPath
	data class Error(val range: Token.Range) : ComplexPath

	fun range() = when (this) {
		is Error -> range
		is Normal -> body.first().id..body.last().let { it.generics?.end ?: it.id }
		is Qualified -> start..body.last().let { it.generics?.end ?: it.id }
	}
}
