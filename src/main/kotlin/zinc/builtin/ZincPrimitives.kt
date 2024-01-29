package zinc.builtin


class ZincNumber(val value: Double) : ZincValue() {
	override val name = "number"
	override fun toString() = value.toString()

	override operator fun plus(b: ZincValue) = ZincNumber(value + (b as ZincNumber).value)
	override operator fun minus(b: ZincValue) = ZincNumber(value - (b as ZincNumber).value)
	override operator fun times(b: ZincValue) = ZincNumber(value * (b as ZincNumber).value)
	override operator fun div(b: ZincValue) = ZincNumber(value / (b as ZincNumber).value)
	override operator fun rem(b: ZincValue) = ZincNumber(value % (b as ZincNumber).value)
}

class ZincChar(val value: Char) : ZincValue() {
	override val name = "char"
	override fun toString() = value.toString()

	fun toNumber() = ZincNumber((value.code).toDouble())
}

class ZincString(val value: String) : ZincValue() {
	override val name = "string"
	override fun toString() = value
	
}

sealed class ZincBoolean(open val value: Boolean) : ZincValue() {
	override val name = "bool"
	override fun toString() = value.toString()
}

data object ZincTrue : ZincBoolean(true)
data object ZincFalse : ZincBoolean(false)