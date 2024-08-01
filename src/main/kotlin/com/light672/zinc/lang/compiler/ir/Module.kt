package com.light672.zinc.lang.compiler.ir

internal class Module(
	name: String,
	val values: Namespace.Branch,
	val types: Namespace.Branch
) : Item(name, 0)