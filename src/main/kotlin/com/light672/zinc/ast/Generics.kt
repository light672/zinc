package com.light672.zinc.ast

internal data class GenericArgs(val start: Token, val types: List<Type>, val end: Token)
internal data class GenericParams(
	val start: Token,
	// val params: List<Pair<Token, TypeParamBounds?>>,
	val params: List<Token>,
	val end: Token
)

internal data class TypeParamBounds(val bounds: List<ComplexPath.Normal>)
internal data class WhereClause(val token: Token, val clauseItems: List<Pair<Type, TypeParamBounds>>)