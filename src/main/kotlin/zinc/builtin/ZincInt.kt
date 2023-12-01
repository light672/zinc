package zinc.builtin

import kotlin.math.pow

data class ZincInt(val value: Int) : ZincValue() {
	override val truthy get() = value != 0
	override operator fun plus(b: ZincValue): ZincValue = ZincInt(value + (b as ZincInt).value)
	override operator fun minus(b: ZincValue): ZincValue = ZincInt(value - (b as ZincInt).value)
	override operator fun times(b: ZincValue): ZincValue = ZincInt(value * (b as ZincInt).value)
	override operator fun div(b: ZincValue): ZincValue = ZincInt(value / (b as ZincInt).value)
	override operator fun rem(b: ZincValue): ZincValue = ZincInt(value % (b as ZincInt).value)
	override fun pow(b: ZincValue): ZincValue = ZincInt(value.pow((b as ZincInt).value))
	override operator fun plusAssign(b: ZincValue) {
		throw IllegalAccessError("plusAssign in $javaClass was illegally called.")
	}

	override operator fun minusAssign(b: ZincValue) {
		throw IllegalAccessError("minusAssign in $javaClass was illegally called.")
	}

	override operator fun timesAssign(b: ZincValue) {
		throw IllegalAccessError("timesAssign in $javaClass was illegally called.")
	}

	override operator fun divAssign(b: ZincValue) {
		throw IllegalAccessError("divAssign in $javaClass was illegally called.")
	}

	override operator fun remAssign(b: ZincValue) {
		throw IllegalAccessError("remAssign in $javaClass was illegally called.")
	}

	override fun powAssign(b: ZincValue) {
		throw IllegalAccessError("powAssign in $javaClass was illegally called.")
	}
}

fun Int.pow(b: Int): Int = this.toDouble().pow(b).toInt()