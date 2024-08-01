package com.light672.zinc.lang.compiler.ir

internal class Variable(name: String, val mutable: Boolean) : Item(name, 0) {
	companion object {
		val ERROR = Variable("", false)
	}
}