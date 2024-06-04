package com.light672.zinc.lang.compiler.parsing.syntax

import com.light672.zinc.builtin.ZincValue

internal sealed class Expr {

	interface WithoutBlock
	class Literal(val literal: ZincValue) : Expr(), WithoutBlock
	class Group(val expr: Expr) : Expr(), WithoutBlock
	class Binary(val a: Expr, val b: Expr, val operator: Token) : Expr(), WithoutBlock
	class Unary(val a: Expr, val operator: Token) : Expr(), WithoutBlock
	class Variable(val variable: Token) : Expr(), WithoutBlock


	interface WithBlock
	class Block(val stmts: List<Stmt>) : Expr(), WithBlock

}