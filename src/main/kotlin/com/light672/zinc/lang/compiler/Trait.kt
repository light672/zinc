package com.light672.zinc.lang.compiler

internal class Trait(val name: String, val functions: ArrayList<Declaration>) {
	companion object {
		val NO_TRAIT = Trait("", ArrayList())
	}
}