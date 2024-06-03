package com.light672.zinc.lang.compiler.parsing.syntax

import com.light672.zinc.builtin.ZincValue

internal sealed class Expr {
	class Literal(val literal: ZincValue) : Expr()
	class Group(val expr: Expr) : Expr()
	class Binary(val a: Expr, val b: Expr, val operator: Token) : Expr()
	class Unary(val a: Expr, val operator: Token) : Expr()
	class Variable(val variable: Token) : Expr()
	class Block : Expr()
}