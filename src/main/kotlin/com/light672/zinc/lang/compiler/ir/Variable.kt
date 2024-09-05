package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.lang.compiler.type_checking.Type

internal open class Variable(val name: String, val mutable: Boolean) : Item() {

	lateinit var type: Type

	companion object {
		val ERROR = Variable("", false)
	}
}