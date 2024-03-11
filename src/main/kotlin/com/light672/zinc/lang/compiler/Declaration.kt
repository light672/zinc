package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Stmt

internal class Declaration(
	val name: String,
	val type: Type,
	val mutable: Boolean,
	val statement: Stmt,
	var initRange: IntRange?,
	val function: Boolean
)