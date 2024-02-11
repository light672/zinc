package zinc.lang.compiler

internal class Scope private constructor(
	val parent: Scope?,
	private val functions: HashMap<Pair<String, Array<Type>>, Type> = HashMap(),
	private val types: HashMap<String, Type> = HashMap(),
	private val variables: HashMap<String, Variable> = HashMap()
) {
	constructor(parent: Scope?) : this(
		parent,
		HashMap<Pair<String, Array<Type>>, Type>(),
		HashMap<String, Type>(),
		HashMap<String, Variable>()
	)

	fun declareVariable(name: String, type: Type) {
		variables[name] = Variable(type)
	}

	fun defineVariable(name: String) {
		variables[name]!!.initialized = true
	}

	fun declareAndDefineVariable(name: String, type: Type) {
		variables[name] = Variable(type, true)
	}

	fun hasVariable(name: String) = variables[name] != null

	fun getType(name: String) = types[name]!!
	fun hasType(name: String) = types[name] != null
	fun declareType(name: String, type: Type) {
		types[name] = type
	}

	fun copy(): Scope {
		if (parent != null) throw IllegalArgumentException("Cannot copy a non-top level scope.")
		return Scope(null, functions.copy(), types.copy(), variables.copy())
	}

	data class Variable(val type: Type, var initialized: Boolean = false)


	private fun <K, V> HashMap<K, V>.copy() = clone() as HashMap<K, V>
}