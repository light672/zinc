package com.light672.zinc.hir

internal sealed interface Type {
	data class ADT(val item: TypeItemRef) : Type
	data class Tuple(val types: List<Type>) : Type
	data class Function(val params: List<Type>, val returnType: List<Type>) : Type

	data object Error : Type
}