package com.light672.zinc.resolution

import com.light672.zinc.ast.ComplexSegment

internal sealed interface ItemReference
internal sealed interface TypeItemReference : ItemReference {
	data class Simple(val item: TypeItem, val generics: GenericArgs?) : TypeItemReference
}

internal sealed interface ValueItemReference : ItemReference {
	data class Simple(val item: ValueItem, val generics: GenericArgs?) : ValueItemReference
	data class TypeAssociated(val type: Type, val segment: ComplexSegment, val generics: GenericArgs?) : ValueItemReference
}

