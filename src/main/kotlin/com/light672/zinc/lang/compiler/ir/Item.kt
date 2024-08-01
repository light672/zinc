package com.light672.zinc.lang.compiler.ir

internal sealed class Item(
	val name: String,
	val genericArity: Int,
)