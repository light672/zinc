package com.light672.zinc.item

import com.light672.zinc.ScopeInfo
import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Type
import com.light672.zinc.ir.IRType

internal sealed interface TypeItem {
	class Module(
		val name: Token,
		val scope: ScopeInfo
	) : TypeItem

	class Struct(
		val name: Token,
		val fields: List<Pair<Token, Type>>
	) : TypeItem {
		lateinit var irFields: Map<CharSequence, IRType>
	}

	class Interface(
		val name: Token,
		val functions: ScopeInfo.Branch<ValueItem>
	) : TypeItem
}