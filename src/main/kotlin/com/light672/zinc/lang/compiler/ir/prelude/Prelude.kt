package com.light672.zinc.lang.compiler.ir.prelude

import com.light672.zinc.lang.compiler.ir.Item

internal object Prelude {
	private val values = HashMap<String, Item>().also {

	}
	private val types = HashMap<String, Item>().also {
		Primitives.register(it)
	}


	fun getType(name: String) = types[name]
	fun getValue(name: String) = values[name]
}