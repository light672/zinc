package zinc.builtin

open class RuntimeException(override val message: String? = null) : kotlin.RuntimeException(message) {
	fun toString(line: Int): String {
		return "[${name()} on line: $line] $message"
	}

	open fun name(): String {
		return "RuntimeException"
	}
}