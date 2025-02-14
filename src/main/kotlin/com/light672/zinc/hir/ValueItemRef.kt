package com.light672.zinc.hir

import com.light672.zinc.ast.Token
import com.light672.zinc.dsr.ValueItem

internal sealed interface ValueItemRef {
	data class Normal(val item: ValueItem, val generics: GenericArgs?) : ValueItemRef
	data class Qualified(
		val type: Type,
		val trait: TypeItemRef.Normal,
		val identifier: Token,
		val genericArgs: GenericArgs?
	) : ValueItemRef

	data class TypeAccess(
		val type: Type,
		val identifier: Token,
		val genericArgs: GenericArgs?
	) : ValueItemRef

	// add enum variant
}