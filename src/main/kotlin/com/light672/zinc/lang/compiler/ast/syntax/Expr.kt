package com.light672.zinc.lang.compiler.ast.syntax

import com.light672.zinc.builtin.ZincValue
import com.light672.zinc.lang.compiler.ir.Namespace

internal sealed class Expr(val firstToken: Token, val lastToken: Token) {

	class Literal(val literal: ZincValue, token: Token) : Expr(token, token)

	class Group(open: Token, val expr: Expr, close: Token) : Expr(open, close)

	class Binary(val a: Expr, val b: Expr, val operator: Token) : Expr(a.firstToken, b.lastToken)

	class Unary(val a: Expr, val operator: Token) : Expr(operator, a.lastToken)

	class Path(firsToken: Token, val path: ComplexPath, lastToken: Token) :
		Expr(firsToken, lastToken)

	class Block(open: Token, val stmts: List<Stmt>, close: Token, val values: Namespace.Branch, val types: Namespace.Branch) : Expr(open, close)


	fun range() = firstToken..lastToken
}