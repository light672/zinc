package zinc.lang.compiler

import zinc.Zinc
import zinc.builtin.ZincBoolean
import zinc.builtin.ZincChar
import zinc.builtin.ZincNumber
import zinc.builtin.ZincString

internal class TypeChecker(val instance: Zinc.Runtime) : Expression.Visitor<Type?> {
	override fun visit(expression: Expression.Binary): Type? {
		val right = expression.right.type()
		val left = expression.left.type()
		if (left === Type.Number) {
			if (right !== Type.Number) {
				instance.reportCompileError("Invalid operands for '${expression.operator.lexeme}', '$left' and '$right'.")
				return null
			}
			return Type.Number
		}
		instance.reportCompileError("Invalid operands for '${expression.operator.lexeme}', '$left' and '$right'.")
		return null
	}

	override fun visit(expression: Expression.Literal): Type {
		return when (expression.value) {
			is ZincNumber -> Type.Number
			is ZincChar -> Type.Char
			is ZincBoolean -> Type.Bool
			is ZincString -> Type.String
			else -> throw IllegalArgumentException()
		}
	}

	override fun visit(expression: Expression.Grouping) = expression.expression.type()

	fun Expression.type(): Type? = accept(this@TypeChecker)

}

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