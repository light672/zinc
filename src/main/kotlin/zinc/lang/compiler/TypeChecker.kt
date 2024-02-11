package zinc.lang.compiler

import zinc.Zinc
import zinc.builtin.ZincBoolean
import zinc.builtin.ZincChar
import zinc.builtin.ZincNumber
import zinc.builtin.ZincString

internal class TypeChecker(val instance: Zinc.Runtime, val resolver: Resolver) {
	fun Expression.Binary.type(): Type? {
		val right = right.type()
		val left = left.type()

		if (left === Type.Number && right === Type.Number) return Type.Number

		instance.reportCompileError("Invalid operands for '${operator.lexeme}', '$left' and '$right'.")
		return null
	}

	fun Expression.Literal.type(): Type {
		return when (value) {
			is ZincNumber -> Type.Number
			is ZincChar -> Type.Char
			is ZincBoolean -> Type.Bool
			is ZincString -> Type.String
			else -> throw IllegalArgumentException()
		}
	}

	fun Expression.GetVariable.type() = resolver.currentScope.getVariable(variable.lexeme).type

	fun Expression.type(): Type? {
		return when (this) {
			is Expression.Literal -> type()
			is Expression.Binary -> type()
			is Expression.Grouping -> expression.type()
			is Expression.GetVariable -> type()
		}
	}

}