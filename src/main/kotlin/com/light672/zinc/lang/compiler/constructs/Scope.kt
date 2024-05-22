package com.light672.zinc.lang.compiler.constructs

internal class Scope {
	val types = HashMap<String, Type>()
	val structs = HashMap<String, Struct>()
	val implements = HashMap<Type, Implement>()
	val traits = HashMap<String, Trait>()
}