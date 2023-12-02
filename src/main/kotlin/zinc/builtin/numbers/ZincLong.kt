package zinc.builtin.numbers

import zinc.builtin.ZincValue

data class ZincLong(val value: Long) : ZincValue() {
	override val truthy get() = value != 0L
	override val name = "long"
	override operator fun plus(b: ZincValue): ZincValue = ZincLong(value + (b as ZincLong).value)
	override operator fun minus(b: ZincValue): ZincValue = ZincLong(value - (b as ZincLong).value)
	override operator fun times(b: ZincValue): ZincValue = ZincLong(value * (b as ZincLong).value)
	override operator fun div(b: ZincValue): ZincValue = ZincLong(value / (b as ZincLong).value)
	override operator fun rem(b: ZincValue): ZincValue = ZincLong(value % (b as ZincLong).value)
}