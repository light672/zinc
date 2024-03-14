package com.light672.zinc.lang.compiler.parsing

internal class ParseRule(
	val precedence: Precedence = Precedence.NONE,
	val prefix: (PrattParser.(Boolean) -> Expr?)? = null,
	val infix: (PrattParser.(Expr, Boolean) -> Expr?)? = null
)