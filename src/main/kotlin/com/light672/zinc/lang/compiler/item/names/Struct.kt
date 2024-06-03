package com.light672.zinc.lang.compiler.item.names

import com.light672.zinc.lang.compiler.Type

internal class Struct(
	name: String,
	val fields: Map<String, Type>,
	val generics: List<Trait>,
	depth: Int
) : Name(name, depth)