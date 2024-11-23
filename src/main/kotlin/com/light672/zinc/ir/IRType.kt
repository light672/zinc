package com.light672.zinc.ir

import com.light672.zinc.item.TypeItem

internal sealed interface IRType {
	data class Item(val typeItem: TypeItem) : IRType
	data class Tuple(val types: List<IRType>) : IRType
	data class Function(val paramTypes: List<IRType>, val returnType: IRType) : IRType
	data object Unit : IRType
	data object Error : IRType


}