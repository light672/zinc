package com.light672.zinc.ast

internal data class GenericArgs(val start: Token, val types: List<Type>, val end: Token)
internal data class GenericParams(
	val start: Token,
	val params: List<GenericParam>,
	val end: Token
)

internal data class GenericParam(val name: Token, val bounds: TypeParamBounds?)

internal data class TypeParamBounds(val bounds: List<ComplexPath.Normal>)
internal data class WhereClause(val token: Token, val clauseItems: List<WhereClauseItem>)
internal data class WhereClauseItem(val type: Type, val bounds: TypeParamBounds)