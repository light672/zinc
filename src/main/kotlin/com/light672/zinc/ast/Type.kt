package com.light672.zinc.ast

internal sealed interface Type {
	data class Tuple(val start: Token, val fields: List<Type>, val end: Token) : Type
	data class Path(val path: ComplexPath) : Type
	data object Error : Type

}