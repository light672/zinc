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
		
		fun expectedStatement(token: Token) =
			CompilerError("expected statement but got `$token`", token.asRange(), "expected statement")

		fun expectedPattern(token: Token) =
			CompilerError("expected pattern but got `$token`", token.asRange(), "expected pattern")

		fun expectedPathSegment(token: Token) =
			CompilerError("expected a path segment leading with `::` after qualified segment", token.asRange(), "expected `::`")

		fun expectedUnqualifiedPath(expr: Expr) =
			CompilerError("expected unqualified path", expr.range(), "expected unqualified path")

		fun expectedItem(expr: Expr) =
			CompilerError("expected item path", expr.range(), "expected item path")

	}
}


