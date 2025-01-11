package com.light672.zinc.ast


internal data class ComplexSegment(val id: Token, val generics: GenericArgs?)
internal sealed interface ComplexPath {
	data class Normal(val body: List<ComplexSegment>) : ComplexPath
	data class Qualified(val start: Token, val type: Type, val trait: Normal?, val end: Token, val body: List<ComplexSegment>) : ComplexPath
	data class Error(val range: Token.Range) : ComplexPath
}
