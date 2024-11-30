package com.light672.zinc.ast

import com.light672.zinc.Scope

internal sealed interface Expr {
	class Literal(
		val token: Token
	) : Expr

	class Variable(
		val path: ComplexPath
	) : Expr

	class Tuple(
		val fields: List<Expr>
	) : Expr

	class Block(
		val stmts: List<Stmt>,
		val scope: Scope
	) : Expr

	class Unary(
		val operator: Token,
		val right: Expr
	) : Expr

	class Binary(
		val left: Expr,
		val operator: Token,
		val right: Expr
	) : Expr

	class Call(
		val callee: Expr,
		val arguments: List<Expr>
	) : Expr

	class Return(
		val expr: Expr?
	)
}