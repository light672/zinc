package zinc.builtin

class ValueError(message: String? = null) : RuntimeException(message) {
	override fun name(): String {
		return "IllegalArgumentException"
	}
}