package zinc.builtin

data class ZincNumber(val value: Double) : ZincValue() {
	override val truthy get() = value != 0.0
}