package com.light672.zinc.item

import com.light672.zinc.ast.Token
import com.light672.zinc.ir.IRPattern
import com.light672.zinc.ir.IRType

internal sealed interface ValueItem {
	class Function : ValueItem {
		lateinit var parameters: List<Pair<IRPattern, IRType>>
		lateinit var returnType: IRType
	}

	class Variable(
		val mutable: Boolean,
		val name: Token,
	) : ValueItem {
		lateinit var type: IRType
	}

	class UnitStruct(
		val struct: TypeItem.Struct // TODO: turn this into a struct item
	) : ValueItem

	data object Ambiguous : ValueItem
}