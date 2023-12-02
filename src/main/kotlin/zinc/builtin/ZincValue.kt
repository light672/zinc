package zinc.builtin

import zinc.builtin.errors.ValueError

open class ZincValue {
	open val name = "Any"
	open val truthy = true
	open operator fun plus(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '+': '$name' and '${b.name}'.")

	open operator fun minus(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '-': '$name' and '${b.name}'.")

	open operator fun times(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '*': '$name' and '${b.name}'.")

	open operator fun div(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '/': '$name' and '${b.name}'.")

	open operator fun rem(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '%': '$name' and '${b.name}'.")

	open fun pow(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '**': '$name' and '${b.name}'.")

	open operator fun plusAssign(b: ZincValue) {
		throw ValueError("Unsupported operand type(s) for '+=': '$name' and '${b.name}'.")
	}

	open operator fun minusAssign(b: ZincValue) {
		throw ValueError("Unsupported operand type(s) for '-=': '$name' and '${b.name}'.")
	}

	open operator fun timesAssign(b: ZincValue) {
		throw ValueError("Unsupported operand type(s) for '*=': '$name' and '${b.name}'.")
	}

	open operator fun divAssign(b: ZincValue) {
		throw ValueError("Unsupported operand type(s) for '/=': '$name' and '${b.name}'.")
	}

	open operator fun remAssign(b: ZincValue) {
		throw ValueError("Unsupported operand type(s) for '%=': '$name' and '${b.name}'.")
	}

	open fun powAssign(b: ZincValue) {
		throw ValueError("Unsupported operand type(s) for '**=': '$name' and '${b.name}'.")
	}

	open operator fun rangeTo(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '..': '$name' and '${b.name}'.")

	open operator fun rangeUntil(b: ZincValue): ZincValue? =
		throw ValueError("Unsupported operand type(s) for '..<': '$name' and '${b.name}'.")

	open operator fun contains(b: ZincValue): Boolean =
		throw ValueError("Unsupported operand type(s) for 'in': '$name' and '${b.name}'.")
}