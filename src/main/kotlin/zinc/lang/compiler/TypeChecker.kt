package zinc.lang.compiler

import zinc.builtin.ZincBoolean
import zinc.builtin.ZincChar
import zinc.builtin.ZincNumber
import zinc.builtin.ZincString

internal class TypeChecker(val resolver: Resolver) {
	fun getType(expression: Expression): Type {
		return when (expression) {
			is Expression.Return -> Type.Nothing
			is Expression.Unit -> Type.Unit
			is Expression.Binary -> Type.Number
			is Expression.Grouping -> getType(expression)
			is Expression.SetVariable -> getType(expression)
			is Expression.GetVariable -> resolver.scopeHandler.getVariableType(expression.variable)!!
			is Expression.Literal -> {
				when (expression.value) {
					is ZincNumber -> Type.Number
					is ZincBoolean -> Type.Bool
					is ZincChar -> Type.Char
					is ZincString -> Type.String
					else -> throw IllegalArgumentException("how did you even mess this up")
				}
			}
		}
	}
}