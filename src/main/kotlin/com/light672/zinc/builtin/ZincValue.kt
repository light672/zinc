package com.light672.zinc.builtin

abstract class ZincValue {
	abstract val name: String
	open operator fun plus(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '+': '$name' and '${b.name}'.")

	open operator fun minus(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '-': '$name' and '${b.name}'.")

	open operator fun times(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '*': '$name' and '${b.name}'.")

	open operator fun div(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '/': '$name' and '${b.name}'.")

	open operator fun rem(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '%': '$name' and '${b.name}'.")

	open fun pow(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '^': '$name' and '${b.name}'.")

	open operator fun plusAssign(b: ZincValue) {
		throw IllegalArgumentException("Unsupported operand type(s) for '+=': '$name' and '${b.name}'.")
	}

	open operator fun minusAssign(b: ZincValue) {
		throw IllegalArgumentException("Unsupported operand type(s) for '-=': '$name' and '${b.name}'.")
	}

	open operator fun timesAssign(b: ZincValue) {
		throw IllegalArgumentException("Unsupported operand type(s) for '*=': '$name' and '${b.name}'.")
	}

	open operator fun divAssign(b: ZincValue) {
		throw IllegalArgumentException("Unsupported operand type(s) for '/=': '$name' and '${b.name}'.")
	}

	open operator fun remAssign(b: ZincValue) {
		throw IllegalArgumentException("Unsupported operand type(s) for '%=': '$name' and '${b.name}'.")
	}

	open fun powAssign(b: ZincValue) {
		throw java.lang.IllegalArgumentException("Unsupported operand type(s) for '^=': '$name and '${b.name}'")
	}

	open operator fun rangeTo(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '..': '$name' and '${b.name}'.")

	open operator fun rangeUntil(b: ZincValue): ZincValue? =
		throw IllegalArgumentException("Unsupported operand type(s) for '..<': '$name' and '${b.name}'.")

	open operator fun contains(b: ZincValue): Boolean =
		throw IllegalArgumentException("Unsupported operand type(s) for 'in': '$name' and '${b.name}'.")
}