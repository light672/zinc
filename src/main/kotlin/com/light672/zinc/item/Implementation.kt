package com.light672.zinc.item

import com.light672.zinc.ScopeInfo
import com.light672.zinc.ast.Type
import com.light672.zinc.ir.IRType

internal class Implementation(
	val type: Type,
	val inheritedInterface: Type?,
	val functions: ScopeInfo.Branch<ValueItem>
) {
	var irInheritedInterface: IRType? = null
	lateinit var irType: IRType
}