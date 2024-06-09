package com.light672.zinc.lang.compiler.resolving.item.names

import com.light672.zinc.lang.compiler.resolving.Type

internal class Trait(
	name: String,
	val generics: List<Trait>,
	val associatedTypes: Map<String, Type>,
	val associatedFunctions: Map<String, Type.Function>,
	depth: Int,
) : Name(name, depth) {
	companion object {
		val DEFAULT_TRAIT = Trait("", ArrayList(), HashMap(), HashMap(), 0)
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		if (!super.equals(other)) return false
		other as Trait
		return generics == other.generics
	}

	override fun hashCode(): Int {
		var result = super.hashCode()
		result = 31 * result + generics.hashCode()
		return result
	}
}