package com.light672.zinc.lang.compiler

internal class Scope(val parent: Scope? = null, val funReturnType: Type? = null) {
	// TODO: make variables return a pair with the index when they are put in
	val types = HashMap<String, Type>()
	val structs = HashMap<String, Struct>()
	val variables = HashMap<String, Declaration>()
	val base: Int = parent?.let { it.base + it.variables.size } ?: 0
}