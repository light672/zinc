package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Stmt

internal class Scope(
	val parent: Scope? = null,
	val type: Type? = parent?.type,
	val base: Int = parent?.let { it.base + it.variables.size } ?: 0
) {
	val types = HashMap<String, Type>()
	val structs = HashMap<String, Struct>()
	val variables = HashMap<String, Pair<Declaration, Int>>()


	fun addVariable(
		name: String,
		type: Type,
		mutable: Boolean,
		statement: Stmt,
		initialized: Boolean,
		function: Boolean,
	): Declaration {
		val declaration = Declaration(name, type, mutable, statement, initialized, function, variables.size + base)
		variables[name] = Pair(declaration, variables.size)
		return declaration
	}


	fun hasLocalVar(name: String) = variables.containsKey(name)
	fun hasLocalStruct(name: String) = structs.containsKey(name)
	fun hasLocalType(name: String) = types.containsKey(name)

	fun getLocalVar(name: String) = variables[name]
	fun getLocalStruct(name: String) = structs[name]
	fun getLocalType(name: String) = types[name]
}