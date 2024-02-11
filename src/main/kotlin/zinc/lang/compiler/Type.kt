package zinc.lang.compiler

sealed class Type {
	object Number : Type() {
		override fun toString() = "num"
	}

	object Char : Type() {
		override fun toString() = "char"
	}

	object Bool : Type() {
		override fun toString() = "bool"
	}

	object String : Type() {
		override fun toString() = "str"
	}

	object Unit : Type() {
		override fun toString() = "unit"
	}

	data class Function(val function: Pair<Array<Type>, Type>) : Type() {
		override fun toString(): kotlin.String {
			val params = StringBuilder("(")
			for (parameterType in function.first) {
				params.append("$parameterType,")
			}
			if (params[params.length - 1] == ',') params.deleteCharAt(params.length - 1)
			params.append(") -> ${function.second}")
			return params.toString()
		}
	}
}