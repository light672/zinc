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

	class Function(val args: Array<Type>, val returnType: Type) : Type() {
		override fun toString() = "function" // TODO: change later
		override fun equals(other: Any?) = other === this || other is Never
	}
}