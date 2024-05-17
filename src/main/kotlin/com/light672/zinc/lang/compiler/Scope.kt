package com.light672.zinc.lang.compiler

internal class Scope {
	val variables = HashMap<String, Variable>()
	val structs = HashMap<String, Struct>()
	val traits = HashMap<String, Trait>()
	val implements = HashMap<Type, Implement>()
	val types = HashMap<String, Type>()
	val namespaces = HashMap<String, Scope>()
}