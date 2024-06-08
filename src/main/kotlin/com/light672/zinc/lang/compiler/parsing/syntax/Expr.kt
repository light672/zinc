package com.light672.zinc.lang.compiler.parsing.syntax

import com.light672.zinc.builtin.ZincValue

internal sealed class Expr(val firstToken: Token, val lastToken: Token) {

	interface WithoutBlock
	class Literal(val literal: ZincValue, token: Token) : Expr(token, token), WithoutBlock
	class Group(open: Token, val expr: Expr, close: Token) : Expr(open, close), WithoutBlock
	class Binary(val a: Expr, val b: Expr, val operator: Token) : Expr(a.firstToken, b.lastToken), WithoutBlock
	class Unary(val a: Expr, val operator: Token) : Expr(operator, a.lastToken), WithoutBlock
	class Path(firsToken: Token, val body: ArrayList<PathExprSegment>, val head: PathExprSegment, lastToken: Token) : Expr(firsToken, lastToken),
		WithoutBlock

	class MutableReference(val mut: Token, val expr: Expr) : Expr(mut, expr.lastToken) // use `expr` as the with our without block check.

	interface WithBlock
	class Block(open: Token, val stmts: List<Stmt>, close: Token) : Expr(open, close), WithBlock


	fun range() = firstToken.range.first..lastToken.range.last
}