package zinc.builtin.numbers

import zinc.builtin.ZincValue

data class ZincShort(val value: Short) : ZincValue() {
	override val truthy get() = value.toInt() != 0
	override val name = "short"
	override operator fun plus(b: ZincValue): ZincValue = ZincInt(value + (b as ZincInt).value)
	override operator fun minus(b: ZincValue): ZincValue = ZincInt(value - (b as ZincInt).value)
	override operator fun times(b: ZincValue): ZincValue = ZincInt(value * (b as ZincInt).value)
	override operator fun div(b: ZincValue): ZincValue = ZincInt(value / (b as ZincInt).value)
	override operator fun rem(b: ZincValue): ZincValue = ZincInt(value % (b as ZincInt).value)
	override fun pow(b: ZincValue): ZincValue = ZincInt(value.toInt().pow((b as ZincInt).value))
}