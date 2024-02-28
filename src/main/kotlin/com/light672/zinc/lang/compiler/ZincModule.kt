package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Statement

internal class ZincModule(
	runtime: com.light672.zinc.Zinc.Runtime,
	val source: String,
	val functions: ArrayList<Statement.Function>,
	val variables: ArrayList<Statement.VariableDeclaration>
) {
	val globals = HashMap<String, Declaration>()
	val types = HashMap<String, Type>()
	val resolver = Resolver(runtime, this)
}