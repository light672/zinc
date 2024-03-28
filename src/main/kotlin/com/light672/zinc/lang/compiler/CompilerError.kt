package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Expr
import com.light672.zinc.lang.compiler.parsing.Token

internal sealed class CompilerError(val message: String) {
	class SimpleError(message: String) : CompilerError(message)
	class TokenError(val token: Token, message: String) : CompilerError(message)

	class OneRangeError(val range: IntRange, message: String) : CompilerError(message)

	class TwoRangeError(
		val rangeA: IntRange,
		val rangeB: IntRange,
		val rangeAMessage: String,
		val rangeBMessage: String,
		message: String
	) : CompilerError(message)

	companion object {
		fun matchingGlobal(a: IntRange, b: IntRange, name: String) = TwoRangeError(
			a, b,
			"'$name' first declared here in top level scope.",
			"'$name' declared again in top level scope.",
			"'$name' can only be declared once in top level scope."
		)

		fun matchingType(a: IntRange, name: String) = OneRangeError(a, "Type '$name' declared twice in the same scope.")
		fun matchingFunctionParameter(a: IntRange, name: String) = OneRangeError(a, "Function parameter '$name' declared twice.")

		fun notMatchingReturnType(expr: Expr, expected: Type, got: Type) =
			OneRangeError(expr.range, "Return type of '$got' does not match expected type '$expected'.")

		fun notMatchingDeclaredType(r: IntRange, a: Type, b: Type) =
			OneRangeError(r, "Declared type of '$a' does not match initializer type of '$b'.")

		fun noGlobalInit(r: IntRange, n: String) = OneRangeError(r, "Global variable '$n' must have an initializer.")

		fun noFieldCalled(s: Type.Struct, f: Token) = TokenError(f, "Field '${f.lexeme}' does not exist in struct '$s'.")
		fun badFieldSetType(field: Pair<IntRange, Type>, exprType: Type, name: Token, expression: Expr, struct: Type.Struct) =
			TwoRangeError(
				field.first, name.range.first..expression.range.last,
				"Declared with type '${field.second}'.",
				"Set with type '$exprType'.",
				"Value being set to '$struct::${name.lexeme}' has type '$exprType', but has type '${field.second}'."
			)

		fun badSetType(variable: Declaration, expr: Expr.SetVariable, expressionType: Type) = TwoRangeError(
			variable.statement.range, expr.range,
			"Declared with type '${variable.type}'.",
			"Set with type '$expressionType'.",
			"Value being set to '${variable.name}' has type '$expressionType', while '${variable.name}' is type '${variable.type}'."
		)

		fun missingFields(r: IntRange, map: HashMap<String, Pair<IntRange, Expr>>, s: Struct) =
			OneRangeError(r, "Struct initialization for '$s' is missing fields ${
				run {
					val builder = StringBuilder()
					for (name in map.keys) {
						builder.append("'$name', ")
					}
					if (builder[builder.length - 2] == ',') {
						builder.delete(builder.length - 2, builder.length)
					}
					builder.toString()
				}
			}.")

		fun badCall(callee: Expr, calleeType: Type) = OneRangeError(callee.range, "Cannot call on '$calleeType'.")
		fun badArgs(expr: Expr, calleeType: Type.Function, argTypes: Array<Type>) =
			OneRangeError(
				expr.range,
				"Function expected argument types '${Type.Function.typeArrayToString(calleeType.parameters)}', but got ${
					Type.Function.typeArrayToString(
						argTypes
					)
				}'."
			)

		fun badDot(expr: Expr, type: Type) = OneRangeError(expr.range, "Cannot get field using '.' on type '$type'.")

		fun noVariable(name: Token) = TokenError(name, "Variable '${name.lexeme}' does not exist in the current scope.")
		fun noType(name: Token) = TokenError(name, "Type '${name.lexeme}' does not exist in the current scope.")
		fun noStruct(name: Token) = TokenError(name, "Struct '${name.lexeme}' does not exist in the current scope.")

		fun usedBeforeInit(name: Token) = TokenError(name, "Variable '${name.lexeme}' used before initialized in all paths.")
		fun badBinaryOperator(expression: Expr.Binary, leftType: Type, rightType: Type) =
			OneRangeError(expression.range, "Cannot perform '${expression.operator.lexeme}' on '$leftType' and '$rightType'.")

		fun badLogicalOperator(expression: Expr.Logical, leftType: Type, rightType: Type) =
			OneRangeError(expression.range, "Cannot perform '${expression.operator.lexeme}' on '$leftType' and '$rightType'.")

		fun badUnaryOperator(expression: Expr.Unary, rightType: Type) =
			OneRangeError(expression.range, "Cannot perform '${expression.operator.lexeme}' on '$rightType'.")
	}
}