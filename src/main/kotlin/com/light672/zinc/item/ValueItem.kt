package com.light672.zinc.item

import com.light672.zinc.ast.Expr
import com.light672.zinc.ast.Pattern
import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Type
import com.light672.zinc.ir.IRExpr
import com.light672.zinc.ir.IRPattern
import com.light672.zinc.ir.IRType

internal sealed interface ValueItem {
	class Function(
		val keyword: Token,
		val name: Token,
		val parameters: List<Pair<Pattern, Type>>,
		val returnType: Type?,
		val block: Expr.Block,
		val parentType: ParentType
	) : ValueItem {
		lateinit var irParameters: List<Pair<IRPattern, IRType>>
		lateinit var irReturnType: IRType
		lateinit var irBlock: IRExpr.Block

		enum class ParentType {
			MODULE,
			IMPL,
			INHERIT_IMPL,
			INTERFACE
		}
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