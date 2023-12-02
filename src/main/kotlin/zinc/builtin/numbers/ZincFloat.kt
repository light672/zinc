package zinc.builtin.numbers

import zinc.builtin.ZincValue
import kotlin.math.pow

data class ZincFloat(val value: Float) : ZincValue() {
	override val truthy get() = value != 0.0f
	override val name = "float"
	override operator fun plus(b: ZincValue): ZincValue = ZincFloat(value + (b as ZincFloat).value)
	override operator fun minus(b: ZincValue): ZincValue = ZincFloat(value - (b as ZincFloat).value)
	override operator fun times(b: ZincValue): ZincValue = ZincFloat(value * (b as ZincFloat).value)
	override operator fun div(b: ZincValue): ZincValue = ZincFloat(value / (b as ZincFloat).value)
	override operator fun rem(b: ZincValue): ZincValue = ZincFloat(value % (b as ZincFloat).value)
	override fun pow(b: ZincValue): ZincValue = ZincFloat(value.pow((b as ZincFloat).value))
}