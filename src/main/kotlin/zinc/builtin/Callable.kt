package zinc.builtin

interface Callable {
	fun call(arity: Int): ZincValue?
}