package com.light672.zinc.item

import com.light672.zinc.Scope
import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Type
import com.light672.zinc.ir.IRType

internal class Implementation(
	val keyword: Token,
	val type: Type,
	val inheritedInterface: Type?,
	val functions: Scope.Branch<ValueItem>
) {
	var irInheritedInterface: IRType? = null
	lateinit var irType: IRType
}