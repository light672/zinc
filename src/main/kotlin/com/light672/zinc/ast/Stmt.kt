package com.light672.zinc.ast

internal sealed interface Stmt {
	class Let(
		val keyword: Token,
		val pattern: Pattern,
		val type: Type?,
		val initializer: Expr?
	) : Stmt

	class Expression(
		val expr: Expr,
		val trailing: Boolean
	) : Stmt
}