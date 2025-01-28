package com.light672.zinc

import com.light672.zinc.ast.Expr
import com.light672.zinc.ast.Token
import com.light672.zinc.ast.TokenType

internal class CompilerError(
	val message: String,
	val range: Token.Range,
	val primaryLabel: String = "",
	val secondaryLabel: String = ""
) {
	companion object {
		val EMPTY = CompilerError("", Token.empty().asRange())

		// lexer
		fun unexpectedChar(char: Char, line: Int, currentOnLine: Int) =
			CompilerError("unexpected char `$char`", Token.empty(line, currentOnLine..currentOnLine).asRange(), "unexpected char")

		fun unterminatedString(char: Char, line: Int, currentOnLine: Int) =
			CompilerError("unterminated string", Token.empty(line, currentOnLine..currentOnLine).asRange(), "expected `\"`")

		// parser

		fun unexpectedToken(token: Token, expected: TokenType) =
			CompilerError("unexpected token `$token`, expected `$expected`", token.asRange(), "expected `$expected`")

		fun unexpectedToken(token: Token, expected: Array<out TokenType>) =
			CompilerError("unexpected token `$token`, expected one of ${expected.asList().map { "`$it`" }.joinToString(" ")}", token.asRange())

		fun expectedPathSegment(expr: Expr) =
			CompilerError("expected path segment in path but got ${expr.name()}", expr.range(), "expected path segment")

		fun expectedType(expr: Expr) =
			CompilerError("expected type but got ${expr.name()}", expr.range(), "expected type")

		fun expectedType(token: Token) =
			CompilerError("expected type but got $token", token.asRange(), "expected type")

		fun expectedExpression(token: Token) =
			CompilerError("expected expression but got `$token`", token.asRange(), "expected expression")

		fun expectedBlockExpr(token: Token) =
			unexpectedToken(token, arrayOf(TokenType.IF, TokenType.WHILE, TokenType.FOR, TokenType.LOOP, TokenType.LEFT_BRACE))

		fun expectedStatement(token: Token) =
			CompilerError("expected statement but got `$token`", token.asRange(), "expected statement")

		fun expectedAssociatedStatement(token: Token) =
			CompilerError(
				"expected function but got `$token`", // TODO: add typealias and const to this once implemented
				token.asRange(),
				"expected associated statement"
			)

		fun expectedPattern(token: Token) =
			CompilerError("expected pattern but got `$token`", token.asRange(), "expected pattern")

		fun expectedPathSegment(token: Token) =
			CompilerError("expected a path segment leading with `::` after qualified segment", token.asRange(), "expected `::`")

		// resolver

		fun nameAlreadyExists(name: CharSequence, declRange: Token.Range, inEnvironment: String) =
			CompilerError("item `$name` already exists in $inEnvironment", declRange, "previously declared")

		fun fieldAlreadyExists(field: Token) =
			CompilerError("field `$field` already exists in struct", field.asRange(), "field name `$field` used more than once")

		fun cannotShadowName(name: CharSequence, declRange: Token.Range) =
			CompilerError("item `$name` cannot be shadowed in scope", declRange, "previously declared")

		fun nameNotFound(name: CharSequence, range: Token.Range, inEnvironment: String) =
			CompilerError("item `$name` does not exist in $inEnvironment", range, "not found in $inEnvironment")

		fun labelNotFound(name: CharSequence, range: Token.Range) =
			CompilerError("label `$name` does not exist in scope", range, "not found in scope")

		fun loopNotFound(expr: Expr) =
			CompilerError("could not find loop for ${expr.name()}", expr.range(), "no loop found, consider using a label")

		fun genericsNotAllowedIn(range: Token.Range, item: String) =
			CompilerError("generic arguments should not be provided for $item", range, "remove generic arguments")

		fun useQualifiedPath(name: Token, range: Token.Range) =
			CompilerError("a qualified path must be used to verify the use of `$name`", range, "use qualified path")

		fun patternMustBeIrrefutable(pattern: Token.Range) =
			CompilerError("pattern must be able to match any value", pattern, "pattern is refutable")

		fun cannotCaptureDynamicEnvironment(token: Token) =
			CompilerError("cannot capture dynamic environment outside of item", token.asRange(), "`$token` is declared outside of item")

		fun cannotUseLabelsOutsideItem(token: Token) =
			CompilerError(
				"cannot use label `$token` as it is defined outside the current function",
				token.asRange(),
				"`$token` is declared outside of item"
			)

		fun itemDoesNotHaveAssociatedItems(name: Token, item: String) =
			CompilerError("item $item does not contain associated items", name.asRange(), "cannot access in $item")

	}
}


