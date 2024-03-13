package com.light672.zinc.lang.compiler.parsing

internal class ParseRule(
	val precedence: Precedence = Precedence.NONE,
	val prefix: (Parser.(Boolean) -> Expr?)? = null,
	val infix: (Parser.(Expr, Boolean) -> Expr?)? = null
)