package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.ParseResult

internal class ZincModule(
	runtime: com.light672.zinc.Zinc.Runtime,
	val source: String,
	val result: ParseResult
) {
	val globals = Scope()
}