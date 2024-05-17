package com.light672.zinc.lang.compiler

internal class Struct(val name: String, val fields: HashMap<String, Pair<Int, Type>>) {
	val type: Type.Object = Type.Object(this)
}