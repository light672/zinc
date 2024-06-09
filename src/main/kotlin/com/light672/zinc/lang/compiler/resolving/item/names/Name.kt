package com.light672.zinc.lang.compiler.resolving.item.names

import com.light672.zinc.lang.compiler.resolving.item.Item

internal open class Name(val name: String, depth: Int) : Item(depth) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		other as Name
		if (name != other.name) return false
		if (depth != other.depth) return false
		return true
	}

	override fun hashCode(): Int {
		var result = name.hashCode()
		result = 31 * result + depth
		return result
	}
}