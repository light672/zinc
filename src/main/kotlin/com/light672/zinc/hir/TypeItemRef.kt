package com.light672.zinc.hir

import com.light672.zinc.ast.Token
import com.light672.zinc.dsr.TypeItem

internal sealed interface TypeItemRef {
	data class Normal(val type: TypeItem, val generics: GenericArgs?) : TypeItemRef
	data class Qualified(
		val type: Type,
		val trait: Normal,
		val identifier: Token,
		val genericArgs: GenericArgs?
	) : TypeItemRef
	// add associated trait variant
}