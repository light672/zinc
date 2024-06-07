package com.light672.zinc.lang.compiler.item.names

import com.light672.zinc.lang.compiler.resolving.Type

internal class Struct(
	name: String,
	val fields: Map<String, Type>,
	val generics: List<Trait>,
	depth: Int
) : Name(name, depth)