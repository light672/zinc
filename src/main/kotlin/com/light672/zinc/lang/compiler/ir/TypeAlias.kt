package com.light672.zinc.lang.compiler.ir

internal class TypeAlias(
	val name: String,
	val genericArity: Int,
) : Item()