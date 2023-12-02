package zinc.builtin.numbers

import zinc.builtin.ZincValue

data class ZincInt(val value: Int) : ZincValue() {
	override val truthy get() = value != 0
	override operator fun plus(b: ZincValue): ZincValue = ZincInt(value + (b as ZincInt).value)
	override operator fun minus(b: ZincValue): ZincValue = ZincInt(value - (b as ZincInt).value)
	override operator fun times(b: ZincValue): ZincValue = ZincInt(value * (b as ZincInt).value)
	override operator fun div(b: ZincValue): ZincValue = ZincInt(value / (b as ZincInt).value)
	override operator fun rem(b: ZincValue): ZincValue = ZincInt(value % (b as ZincInt).value)
	override fun pow(b: ZincValue): ZincValue = ZincInt(value.pow((b as ZincInt).value))
}