package zinc.builtin.numbers

import zinc.builtin.ZincValue
import kotlin.math.pow

data class ZincDouble(val value: Double) : ZincValue() {
	override val truthy get() = value != 0.0
	override operator fun plus(b: ZincValue): ZincValue = ZincDouble(value + (b as ZincDouble).value)
	override operator fun minus(b: ZincValue): ZincValue = ZincDouble(value - (b as ZincDouble).value)
	override operator fun times(b: ZincValue): ZincValue = ZincDouble(value * (b as ZincDouble).value)
	override operator fun div(b: ZincValue): ZincValue = ZincDouble(value / (b as ZincDouble).value)
	override operator fun rem(b: ZincValue): ZincValue = ZincDouble(value % (b as ZincDouble).value)
	override fun pow(b: ZincValue): ZincValue = ZincDouble(value.pow((b as ZincDouble).value))
}