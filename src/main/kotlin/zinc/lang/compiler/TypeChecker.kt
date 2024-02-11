package zinc.lang.compiler

import zinc.Zinc
import zinc.builtin.ZincBoolean
import zinc.builtin.ZincChar
import zinc.builtin.ZincNumber
import zinc.builtin.ZincString

internal class TypeChecker(val instance: Zinc.Runtime, val resolver: Resolver) : Expression.Visitor<Type?> {
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
	override fun visit(expression: Expression.GetVariable) =
		resolver.currentScope.getVariable(expression.variable.lexeme).type

	fun Expression.type(): Type? = accept(this@TypeChecker)

}