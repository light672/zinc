package com.light672.zinc.lang.compiler.type_checking

import com.light672.zinc.lang.compiler.ir.Item

internal sealed class Type {
	data class Normal(val item: Item?, val generics: List<Type>) : Type()
	data class Tuple(val fields: List<Type>) : Type()
	companion object {
		val UNIT = Normal(null, emptyList())
		val NEVER = Normal(null, emptyList())
		val ERROR = Normal(null, emptyList())

		val UNDEFINED = Normal(null, emptyList())
	}
}