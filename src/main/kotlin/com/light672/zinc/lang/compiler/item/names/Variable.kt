package com.light672.zinc.lang.compiler.item.names

import com.light672.zinc.lang.compiler.Type

internal class Variable(
	name: String,
	val type: Type,
	var initialized: Boolean,
	depth: Int
) : Name(name, depth)