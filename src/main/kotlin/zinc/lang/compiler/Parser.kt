package zinc.lang.compiler

import zinc.builtin.ZincFalse
import zinc.builtin.ZincTrue
import zinc.builtin.numbers.ZincLong
import zinc.lang.compiler.Token.Type.*
import java.lang.Integer.parseInt
import java.lang.Long.parseLong

class Parser(private val source: String) {
	private val lexer: Lexer = Lexer(source)
	private var current: Token = Token.empty()
	var previous: Token = Token.empty()

	private fun parse() = ArrayList<Statement>().also { while (!end()) it.add(parseStatement()) }
	private fun block() = ArrayList<Statement>().also { }

	private fun parseStatement(): Statement {
		return declaration() ?: synchronize()
	}

	private fun declaration(): Statement? {
		return if (match(VAR)) varDeclaration()
		else if (match(FUNC)) funcDeclaration()
		else statement()
	}

	private fun statement(): Statement? {
		return if (match(WHILE)) whileStatement()
		else if (match(FOR)) forStatement()
		else if (match(LOOP)) loopStatement()
		else if (match(RETURN)) returnStatement()
		else expressionStatement()
	}

	private fun expressionStatement(): Statement.ExpressionStatement? {
		val expression = expression() ?: return null
		consume(SEMICOLON, "Expected ';' after expression.")
		return Statement.ExpressionStatement(expression)
	}

	private fun expression() = assignment()

	private fun assignment(): Expression? {
		val expression = or() ?: return null
		if (match(EQUAL)) {
			val equals = previous
			val value = assignment() ?: return null
			if (expression is Expression.Variable) return Expression.Assign(expression, value)

			errorAt(equals, "Invalid assignment target.")
			return null
		}
		return expression
	}

	private fun parseBinaryExpression(next: () -> Expression?, vararg types: Token.Type): Expression? {
		var expression = next() ?: return null
		while (match(types)) {
			val operator = previous
			val right = next() ?: return null
			expression = Expression.Binary(expression, right, operator)
		}
		return expression
	}

	private fun or() = parseBinaryExpression({ and() }, OR)
	private fun and() = parseBinaryExpression({ equality() }, AND)
	private fun equality() = parseBinaryExpression({ comparison() }, BANG_EQUAL, EQUAL_EQUAL)
	private fun comparison() = parseBinaryExpression({ modulo() }, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)
	private fun modulo() = parseBinaryExpression({ term() }, PERCENT)
	private fun term() = parseBinaryExpression({ factor() }, MINUS, PLUS)
	private fun factor() = parseBinaryExpression({ unary() }, SLASH, STAR)
	private fun unary(): Expression? {
		if (match(BANG, MINUS)) {
			val operator = previous
			val right = unary() ?: return null
			return Expression.Unary(right, operator)
		}
		return primary()
	}

	private fun primary(): Expression? {
		if (match(FALSE)) return Expression.Literal(ZincFalse)
		if (match(TRUE)) return Expression.Literal(ZincTrue)
		if (match(NIL)) return Expression.Literal(null)
		Double.MAX_VALUE
		if (match(NUMBER_VALUE)) {
			try {
				val num = parseInt(previous.lexeme)
				
			} catch (e: NumberFormatException) {
				println("Primary parsing for integer failed, ${e.message}")
				val num = parseLong(previous.lexeme)
				return (Expression.Literal(ZincLong(num)))
			}
		}
	}


	private fun advance(): Unit? {
		previous = current
		current = lexer.scanToken()
		if (current.type == ERROR) return errorAtCurrent(current.lexeme)
		return Unit
	}

	private fun consume(type: Token.Type, message: String): Unit? {
		if (!match(type)) return errorAtCurrent(message)
		return Unit
	}

	private fun match(type: Token.Type) =
		if (isNext(type)) {
			advance()
			true
		} else false

	private fun match(types: Array<out Token.Type>) =
		if (current.type in types) {
			advance()
			true
		} else false

	private fun match(vararg types: Token.Type) =
		if (current.type in types) {
			advance()
			true
		} else false

	private fun end() = current.type == EOF
	private fun isNext(type: Token.Type) = current.type == type
	private fun isNext(vararg types: Token.Type) = current.type in types

	private fun error(message: String) = errorAt(previous, message)
	private fun errorAtCurrent(message: String) = errorAt(current, message)
	private fun errorAt(token: Token, message: String): Unit? {
		val message =
			"Error ${if (token.type == EOF) "at end" else if (token.type != ERROR) "at ${token.lexeme}" else "with scanning: '$message'."}"
		return null
	}
}