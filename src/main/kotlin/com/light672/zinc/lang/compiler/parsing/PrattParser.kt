package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.CompilerError
import com.light672.zinc.lang.compiler.parsing.Token.Type.*
import java.lang.Double.parseDouble

internal class PrattParser(source: String, private val runtime: Zinc.Runtime) {
	private val lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()


	fun unary(canAssign: Boolean): Expr.Unary? {
		return Expr.Unary(previous, parsePrecedence(Precedence.UNARY) ?: return null)
	}

	fun stringLiteral(u: Boolean) = Expr.Literal(ZincString(previous.lexeme), previous)
	fun charLiteral(u: Boolean) = Expr.Literal(ZincChar(previous.lexeme[0]), previous)
	fun numberLiteral(u: Boolean) = Expr.Literal(ZincNumber(parseDouble(previous.lexeme)), previous)
	fun trueLiteral(u: Boolean) = Expr.Literal(ZincTrue, previous)
	fun falseLiteral(u: Boolean) = Expr.Literal(ZincFalse, previous)

	private fun parsePrecedence(precedence: Precedence): Expr? {
		advance()
		val rule = previous.type.rule.prefix
		if (rule == null) {
			error("Expected expression.")
			return null
		}
		val canAssign = precedence.ordinal <= Precedence.ASSIGNMENT.ordinal
		var left = rule(canAssign) ?: return null
		while (precedence.ordinal <= current.type.rule.precedence.ordinal) {
			advance()
			val infix = previous.type.rule.infix!!
			left = infix(left, canAssign) ?: return null
		}
		if (canAssign && match(EQUAL)) {
			error("Invalid assignment target.")
			return null
		}
		return left
	}

	private fun advance(): Unit? {
		previous = current; current = lexer.scanToken(); return if (current.type == ERROR) {
			errorAtCurrent(current.lexeme); null
		} else Unit
	}

	private fun expect(type: Token.Type, message: String) = if (!match(type)) {
		errorAtCurrent(message); null
	} else Unit

	private fun match(type: Token.Type) = if (isNext(type)) {
		advance(); true
	} else false

	private fun match(types: Array<out Token.Type>) = if (current.type in types) {
		advance(); true
	} else false

	private fun end() = current.type == EOF
	private fun isNext(type: Token.Type) = current.type == type
	private fun isNext(vararg types: Token.Type) = current.type in types
	private fun error(message: String) = errorAt(previous, message)
	private fun errorAtCurrent(message: String) = errorAt(current, message)
	private fun errorAt(token: Token, message: String) = runtime.reportCompileError(CompilerError.TokenError(token, message))


	class ParseRule(
		val precedence: Precedence = Precedence.NONE,
		val prefix: (PrattParser.(Boolean) -> Expr?)? = null,
		val infix: (PrattParser.(Expr, Boolean) -> Expr?)? = null
	)

	enum class Precedence {
		NONE,
		ASSIGNMENT,
		TERNARY,
		OR,
		AND,
		EQUALITY,
		COMPARISON,
		TERM,
		FACTOR,
		EXPONENT,
		UNARY,
		INIT,
		CALL
	}
}