package zinc.builtin

data class ZincBoolean(val value: Boolean) : ZincValue() {
	override val truthy get() = value
}