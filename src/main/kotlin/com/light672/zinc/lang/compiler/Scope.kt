package com.light672.zinc.lang.compiler

internal class Scope(val parent: Scope? = null, val funReturnType: Type? = null) {
	val types = HashMap<String, Type>()
	val structs = HashMap<String, Struct>()
	val variables = HashMap<String, Pair<Declaration, Int>>()
	val base: Int = parent?.let { it.base + it.variables.size } ?: 0

	fun addVariable(name: String, declaration: Declaration) {
		variables[name] = Pair(declaration, variables.size)
	}
}