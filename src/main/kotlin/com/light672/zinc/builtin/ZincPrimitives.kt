package com.light672.zinc.builtin

import kotlin.math.pow


data class ZincNumber(val value: Double) : ZincValue() {
	override fun toString() = value.toString()

	operator fun plus(b: ZincNumber) = ZincNumber(value + b.value)
	operator fun minus(b: ZincNumber) = ZincNumber(value - b.value)
	operator fun times(b: ZincNumber) = ZincNumber(value * b.value)
	operator fun div(b: ZincNumber) = ZincNumber(value / b.value)
	operator fun rem(b: ZincNumber) = ZincNumber(value % b.value)
	fun pow(b: ZincNumber) = ZincNumber(value.pow(b.value))
}

data class ZincChar(val value: Char) : ZincValue() {
	override fun toString() = value.toString()

	fun toNumber() = ZincNumber((value.code).toDouble())

	operator fun plus(b: ZincChar) = ZincString(value.toString() + b)
	operator fun plus(b: ZincString) = ZincString(value.toString() + b)
}

data class ZincString(val value: String) : ZincValue() {
	override fun toString() = value
	operator fun plus(b: ZincChar) = ZincString(value + b)
	operator fun plus(b: ZincString) = ZincString(value + b)
}

data class ZincGroup(val array: Array<ZincValue?>) : ZincValue() {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as ZincGroup
		return array.contentEquals(other.array)

	}

	override fun hashCode(): Int {
		return array.contentHashCode()
	}
}

open class ZincBoolean(open val value: Boolean) : ZincValue() {
	override fun toString() = value.toString()
}

data object ZincTrue : ZincBoolean(true)
data object ZincFalse : ZincBoolean(false)

