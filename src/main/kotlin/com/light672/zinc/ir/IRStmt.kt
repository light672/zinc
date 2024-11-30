package com.light672.zinc.ir

import com.light672.zinc.Scope
import com.light672.zinc.item.ValueItem

internal sealed interface IRStmt {
	class Let(
		val values: Scope.Branch<ValueItem>,
		val pattern: IRPattern,
		var type: IRType?,
		val initializer: IRExpr?,
	) : IRStmt

	class Expression(
		val expression: IRExpr,
		val trailing: Boolean
	) : IRStmt
}