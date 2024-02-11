package zinc.lang.compiler

import zinc.Zinc

internal class Scope(
	val parent: Scope?,
	private val functions: HashMap<Pair<String, Array<Type>>, Type> = HashMap(),
	private val types: HashMap<String, Type> = HashMap(),
	private val variables: HashMap<String, Variable> = HashMap()
) {

	fun declareVariable(name: String, type: Type, mutable: Boolean) {
		variables[name] = Variable(type, mutable)
	}

	fun defineVariable(name: String) {
		variables[name]!!.initialized = true
	}

	fun declareAndDefineType(name: String, type: Type) {
		types[name] = type
	}

	fun getVariableOrNull(name: String): Variable? = variables[name] ?: parent?.getVariableOrNull(name)

	fun getTypeOrNull(name: String): Type? = types[name] ?: parent?.getTypeOrNull(name)

	fun defineAndDeclareFunction(
		name: String,
		parameters: Array<Pair<String, Type>>,
		returnType: Type,
		instance: Zinc.Runtime
	) {
		fun toString(): String {
			val params = StringBuilder("$name (")
			for (parameterType in parameters) {
				params.append("${parameterType.second},")
			}
			if (params[params.length - 1] == ',') params.deleteCharAt(params.length - 1)
			params.append(") -> $returnType")
			return params.toString()
		}


		if (functions[Pair(
				name,
				ArrayList<Type>().also { for (param in parameters) it.add(param.second) }.toTypedArray()
			)] != null
		) {
			instance.reportCompileError("Function '${toString()}' has already defined in the current scope.")
			return
		}
		functions[Pair(
			name,
			ArrayList<Type>().also { for (param in parameters) it.add(param.second) }.toTypedArray()
		)] = returnType
	}

	fun copy(): Scope {
		if (parent != null) throw IllegalArgumentException("Cannot copy a non-top level scope.")
		return Scope(null, functions.copy(), types.copy(), variables.copy())
	}

	data class Variable(val type: Type, val mutable: Boolean, var initialized: Boolean = false)


	private fun <K, V> HashMap<K, V>.copy() = clone() as HashMap<K, V>
}