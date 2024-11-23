package com.light672.zinc.ir

import com.light672.zinc.ast.Expr
import com.light672.zinc.ast.TokenType
import com.light672.zinc.item.ValueItem

internal sealed interface IRExpr {
	class Literal(
		val value: Any
	) : IRExpr

	class Variable(
		val variable: ValueItem
	) : IRExpr

	class Tuple(
		val fields: List<IRExpr>
	) : IRExpr

	class Block(
		val ast: Expr.Block,
		val stmts: List<IRStmt>
	) : IRExpr

	class Unary(
		val operator: TokenType,
		val right: IRExpr
	) : IRExpr

	class Binary(
		val left: IRExpr,
		val operator: TokenType,
		val right: IRExpr
	) : IRExpr

	class Call(
		val callee: IRExpr,
		val arguments: List<IRExpr>
	) : IRExpr

	class Return(
		val expr: IRExpr
	) : IRExpr

	object Error : IRExpr
}