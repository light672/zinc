package com.light672.zinc.lang.compiler.item.names

import com.light672.zinc.lang.compiler.resolving.Type

internal class Variable(
	name: String,
	val type: Type,
	var initialized: Boolean,
	depth: Int
) : Name(name, depth)