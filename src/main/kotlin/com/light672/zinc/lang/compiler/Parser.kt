package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.Token.Type.*
import java.lang.Double.parseDouble

internal class Parser(source: String, private val runtime: Zinc.Runtime) {
	class ParseError : RuntimeException()

	private fun declaration() = statement()
	private fun statement(): Stmt = expressionStmt()
	private fun expressionStmt(): Stmt {
		val expression = expression()
		expect(SEMICOLON, "Expected ';' after statement.")
		return Stmt.Expression(expression)
	}

	fun charLiteral() = Expr.Literal(ZincChar(previous.lexeme[0]))
	fun stringLiteral() = Expr.Literal(ZincString(previous.lexeme))
	fun numberLiteral() = Expr.Literal(ZincNumber(parseDouble(previous.lexeme)))
	fun trueLiteral() = Expr.Literal(ZincTrue)
	fun falseLiteral() = Expr.Literal(ZincFalse)

	fun parenthesis(): Expr {
		val expr = expression()
		expect(RIGHT_PAREN, "Expected ')' after expression.")
		return Expr.Group(expr)
	}

	private fun binary(a: Expr, precedence: Precedence): Expr {
		val operator = previous
		val b = parsePrecedence(precedence)
		return Expr.Binary(a, b, operator)
	}

	fun or(a: Expr) = binary(a, Precedence.AND)
	fun and(a: Expr) = binary(a, Precedence.EQUALITY)
	fun equality(a: Expr) = binary(a, Precedence.COMPARISON)
	fun comparison(a: Expr) = binary(a, Precedence.TERM)
	fun term(a: Expr) = binary(a, Precedence.FACTOR)
	fun factor(a: Expr) = binary(a, Precedence.EXPONENT)
	fun exponent(a: Expr) = binary(a, Precedence.UNARY)
	fun unary(): Expr {
		val operator = previous
		val expr = parsePrecedence(Precedence.UNARY)
		return Expr.Unary(expr, operator)
	}

	// <editor-fold desc="normal parsing stuff">
	private val lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()

	private fun advance() {
		previous = current
		current = lexer.scanToken()
		if (current.type == ERROR) throw errorAtCurrent(current.lexeme)
	}

	private fun expect(type: Token.Type, message: String) {
		if (!match(type)) throw errorAtCurrent(message)
	}

	private fun match(type: Token.Type) =
		if (isNext(type)) {
			advance()
			true
		} else false

	private fun end() = isNext(EOF)
	private fun isNext(type: Token.Type) = current.type == type
	private fun isNext(vararg types: Token.Type) = current.type in types
	private fun error(message: String) = errorAt(previous, message)
	private fun errorAtCurrent(message: String) = errorAt(current, message)
	private fun errorAt(token: Token, message: String): ParseError {
		runtime.reportCompileError(message)
		return ParseError()
	}

	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT)

	private fun parsePrecedence(precedence: Precedence): Expr {
		advance()
		val rule = previous.type.rule.prefix ?: throw error("Expected expression.")
		val canAssign = precedence.ordinal <= Precedence.ASSIGNMENT.ordinal
		var left = rule()
		while (precedence.ordinal <= current.type.rule.precedence.ordinal) {
			advance()
			val infix = previous.type.rule.infix!!
			left = infix(left)
		}
		if (canAssign && match(EQUAL)) throw error("Invalid assignment target.")
		return left
	}

	fun parse() {
		val statements = ArrayList<Stmt>()
		advance()
		while (!end()) {
			try {
				statements.add(declaration())
			} catch (error: ParseError) {
			}
		}
	}

	// </editor-fold>
}