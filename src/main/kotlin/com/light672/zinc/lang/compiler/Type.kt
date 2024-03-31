package com.light672.zinc.lang.compiler

internal sealed class Type {
	object Number : Type() {
		override fun toString() = "num"
		override fun equals(other: Any?) = other is Number || other is Never
	}

	object Char : Type() {
		override fun toString() = "char"
		override fun equals(other: Any?) = other is Char || other is Never
	}

	object Bool : Type() {
		override fun toString() = "bool"
		override fun equals(other: Any?) = other is Bool || other is Never
	}

	object String : Type() {
		override fun toString() = "str"
		override fun equals(other: Any?) = other is String || other is Never
	}

	object Unit : Type() {
		override fun toString() = "()"
		override fun equals(other: Any?) = other is Unit || other is Never
	}

	object Never : Type() {
		override fun toString() = "nothing"
		override fun equals(other: Any?) = other is Type
	}

	data class Function(val parameters: Array<Type>, val returnType: Type) : Type() {
		override fun toString(): kotlin.String = "${typeArrayToString(parameters)} -> $returnType"

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other === Never) return true
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

	class Struct(val name: kotlin.String, val fields: LinkedHashMap<kotlin.String, Pair<IntRange, Type>>) : Type() {
		override fun toString(): kotlin.String = name
		override fun equals(other: Any?) = this === other || other is Never
	}
}