package com.light672.zinc.lang.compiler

internal class Scope(val parent: Scope? = null, val funReturnType: Type? = null) {
	val types = HashMap<String, Type>()
	val structs = HashMap<String, Struct>()
	val variables = HashMap<String, Declaration>()
}