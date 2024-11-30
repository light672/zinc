package com.light672.zinc.item

import com.light672.zinc.Scope
import com.light672.zinc.ast.Token
import com.light672.zinc.ir.IRType

internal sealed interface TypeItem {
	class Module(
		val name: Token,
		val scope: Scope
	) : TypeItem

	class Struct : TypeItem {
		lateinit var fields: Map<CharSequence, IRType>
	}

	class Interface(
		val name: Token,
		val functions: Map<CharSequence, ValueItem.Function>
	) : TypeItem
}