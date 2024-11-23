package com.light672.zinc.ir

import com.light672.zinc.ScopeInfo
import com.light672.zinc.item.ValueItem

internal sealed interface IRStmt {
	class Let(
		val values: ScopeInfo.Branch<ValueItem>,
		val pattern: IRPattern,
		var type: IRType?,
		val initializer: IRExpr?,
	) : IRStmt

	class Expression(
		val expression: IRExpr,
		val trailing: Boolean
	) : IRStmt
}