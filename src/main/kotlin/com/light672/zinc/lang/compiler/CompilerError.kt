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

		fun notMatchingDeclaredType(r: IntRange, a: Type, b: Type) =
			OneRangeError(r, "Declared type of '$a' does not match initializer type of '$b'.")

		fun noGlobalInit(r: IntRange, n: String) = OneRangeError(r, "Global variable '$n' must have an initializer.")

		fun noFieldCalled(s: Type.Struct, f: Token) = TokenError(f, "Field '${f.lexeme}' does not exist in struct '$s'.")
		fun badFieldSetType(field: Pair<IntRange, Type>, exprType: Type, name: Token, expression: Expr, struct: Type.Struct) =
			TwoRangeError(
				field.first, name.range.first..expression.range.last,
				"Declared with type '${struct.fields[name.lexeme]!!.second}'.",
				"Set with type '$exprType'.",
				"Value being set to '$struct::${name.lexeme}' has type '$exprType', while '$struct::${name.lexeme}' is type '${field.second}'."
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
	}
}