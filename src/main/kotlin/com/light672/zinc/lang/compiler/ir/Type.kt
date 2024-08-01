package com.light672.zinc.lang.compiler.ir

internal class Type(val item: Item?, val genericArgs: List<Type>) {
	companion object {
		val ERROR = Type(null, emptyList())
		val UNIT = Type(null, emptyList())
		val NEVER = Type(null, emptyList())
	}
}