package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Stmt

internal class ZincModule(
	runtime: com.light672.zinc.Zinc.Runtime,
	val source: String,
	val structs: ArrayList<Stmt.Struct>,
	val functions: ArrayList<Stmt.Function>,
	val variables: ArrayList<Stmt.VariableDeclaration>
) {
	val globals = Scope()
}