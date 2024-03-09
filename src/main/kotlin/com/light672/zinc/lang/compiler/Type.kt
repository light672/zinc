package com.light672.zinc.lang.compiler

sealed class Type {
	data object Number : Type() {
		override fun toString() = "num"
	}

	data object Char : Type() {
		override fun toString() = "char"
	}

	data object Bool : Type() {
		override fun toString() = "bool"
	}

	data object String : Type() {
		override fun toString() = "str"
	}

	data object Unit : Type() {
		override fun toString() = "()"
	}

	data object Nothing : Type() {
		override fun toString() = "nothing"
	}

	data class Function(val parameters: Array<Type>, val returnType: Type) : Type() {
		override fun toString(): kotlin.String = "${typeArrayToString(parameters)} -> $returnType"

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Function

			if (!parameters.contentEquals(other.parameters)) return false
			if (returnType != other.returnType) return false

			return true
		}

		override fun hashCode(): Int {
			var result = parameters.contentHashCode()
			result = 31 * result + returnType.hashCode()
			return result
		}

		companion object {
			fun typeArrayToString(parameters: Array<Type>): kotlin.String {
				val params = StringBuilder("(")
				for (parameterType in parameters) {
					params.append("$parameterType, ")
				}
				if (params[params.length - 2] == ',') {
					params.delete(params.length - 2, params.length)
				}
				params.append(")")
				return params.toString()
			}
		}
	}

	class Struct(val name: kotlin.String, val fields: HashMap<kotlin.String, Pair<IntRange, Type>>) : Type() {
		override fun toString(): kotlin.String = name
	}
}