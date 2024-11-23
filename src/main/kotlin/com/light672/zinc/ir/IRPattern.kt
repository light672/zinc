package com.light672.zinc.ir


import com.light672.zinc.item.ValueItem

internal sealed class IRPattern(
	val refutable: Boolean
) {
	class Identifier(
		val createdVariable: ValueItem.Variable
	) : IRPattern(true)

	class Tuple(
		val fields: List<IRPattern>
	) : IRPattern(fields.all { it.refutable })

	class Path(
		val item: ValueItem
	) : IRPattern(item is ValueItem.UnitStruct)

	data object Underscore : IRPattern(false)
	data object Error : IRPattern(false)
}