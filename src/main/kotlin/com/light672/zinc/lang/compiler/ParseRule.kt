package com.light672.zinc.lang.compiler

internal class ParseRule(
	val precedence: Precedence = Precedence.NONE,
	val prefix: (Compiler.(Boolean) -> ExprData?)? = null,
	val infix: (Compiler.(ExprData, Boolean) -> ExprData?)? = null
)