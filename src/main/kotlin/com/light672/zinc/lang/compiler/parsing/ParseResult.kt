package com.light672.zinc.lang.compiler.parsing

internal data class ParseResult(
	val structs: ArrayList<Stmt.Struct>,
	val functions: ArrayList<Stmt.Function>,
	val variables: ArrayList<Stmt.VariableDeclaration>,
	val impls: ArrayList<Stmt.Impl>
)