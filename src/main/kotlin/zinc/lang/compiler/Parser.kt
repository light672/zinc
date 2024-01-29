package zinc.lang.compiler

import zinc.Zinc
import zinc.builtin.*
import zinc.lang.compiler.Token.Type.*
import java.lang.Double.parseDouble

internal class Parser(source: String, private val instance: Zinc.Runtime) {
	private val lexer: Lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()

	internal fun parse() = ArrayList<Statement>().also {
		advance()
		while (!end()) it.add(parseStatement())
	}


	private fun parseStatement() = declaration() ?: throw RuntimeException()

	private fun declaration(): Statement? {
		return statement()
	}

	private fun statement(): Statement? {
		return expressionStatement();
	}

	private fun expressionStatement(): Statement.ExpressionStatement? {
		val expression = expression() ?: return null
		expect(SEMICOLON, "Expected ';' after expression.")
		return Statement.ExpressionStatement(expression)
	}

	private fun expression() = equality()

	private fun equality() = parseBinaryExpression({ comparison() }, BANG_EQUAL, EQUAL_EQUAL)
	private fun comparison() = parseBinaryExpression({ modulo() }, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)
	private fun modulo() = parseBinaryExpression({ term() }, PERCENT)
	private fun term() = parseBinaryExpression({ factor() }, MINUS, PLUS)
	private fun factor() = parseBinaryExpression({ primary() }, SLASH, STAR)

	/**
	 * Returns literals and groups.
	 * Errors already handled, returns null if an error was found and processed.
	 */
	private fun primary(): Expression? {

		if (match(FALSE)) return Expression.Literal(ZincFalse)
		if (match(TRUE)) return Expression.Literal(ZincTrue)
		if (match(NIL)) return Expression.Literal(null)
		if (match(NUMBER_VALUE)) return Expression.Literal(ZincNumber(parseDouble(previous.lexeme)))
		if (match(STRING_VALUE)) return Expression.Literal(ZincString(previous.lexeme))
		if (match(CHAR_VALUE)) {
			if (previous.lexeme.length != 1) {
				error(
					if (previous.lexeme.length > 1) "Too many characters in a character literal '${previous.lexeme}'."
					else "Cannot have an empty character literal."
				)
				return null
			} else return Expression.Literal(ZincChar(previous.lexeme[0]))
		}
		if (match(LEFT_PAREN)) {
			val expression = expression() ?: return null
			expect(RIGHT_PAREN, "Expect ')' after expression.") ?: return null
			return Expression.Grouping(expression)
		}

		errorAtCurrent("Expected expression.")
		return null
	}

	private fun parseBinaryExpression(next: () -> Expression?, vararg types: Token.Type): Expression? {
		val expression = next() ?: return null
		while (match(types)) {
			val operator = previous
			val right = next() ?: return null
			return Expression.Binary(expression, right, operator)
		}
		return expression
	}

	/**
	 * Advances the lexer by one token.
	 * Errors already handled, returns null if an error was found and processed.
	 */
	private fun advance(): Unit? {
		previous = current
		current = lexer.scanToken()
		if (current.type == ERROR) {
			errorAtCurrent(current.lexeme)
			return null
		}

		return Unit
	}

	/**
	 * Expects a specific token or handles an error.
	 * Errors already handled, returns null if an error was found and processed.
	 */
	private fun expect(type: Token.Type, message: String): Unit? {
		if (!match(type)) {
			errorAtCurrent(message)
			return null
		}
		return Unit
	}

	/**
	 * Checks if the next token matches the given token, and advances if they do match.
	 */
	private fun match(type: Token.Type) =
		if (isNext(type)) {
			advance()
			true
		} else false

	/**
	 * Checks if the next token matches one of the given tokens, and advances if they do match.
	 */
	private fun match(types: Array<out Token.Type>) =
		if (current.type in types) {
			advance()
			true
		} else false

	/**
	 * Checks if the lexer has reached the end of the file.
	 */
	private fun end() = current.type == EOF

	/**
	 * Checks if the next token matches the given token.
	 */
	private fun isNext(type: Token.Type) = current.type == type

	/**
	 * Checks if the next token matches one of the given tokens.
	 */
	private fun isNext(vararg types: Token.Type) = current.type in types

	private fun error(message: String) = errorAt(previous, message)
	private fun errorAtCurrent(message: String) = errorAt(current, message)
	private fun errorAt(token: Token, message: String) {
		instance.reportCompileError(
			"Error ${
				if (token.type == EOF) "at end"
				else if (token.type != ERROR) "at '${token.lexeme}'"
				else "with scanning"
			}: '$message'."
		)
	}
}
