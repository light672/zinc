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

	fun expression() = parsePrecedence(Precedence.ASSIGNMENT)

	fun unary(u: Boolean): Expr.Unary? {
		return Expr.Unary(previous, parsePrecedence(Precedence.UNARY) ?: return null)
	}

	fun grouping(u: Boolean): Expr.Grouping? {
		val p = previous
		val expression = expression() ?: return null
		expect(RIGHT_PAREN, "Expected ')' after expression.") ?: return null
		return Expr.Grouping(expression, p, previous)
	}

	fun variable(canAssign: Boolean): Expr? {
		val name = previous
		if (!canAssign || !match(EQUAL)) return Expr.GetVariable(name)
		return Expr.SetVariable(name, expression() ?: return null)
	}


	fun stringLiteral(u: Boolean) = Expr.Literal(ZincString(previous.lexeme), previous)
	fun charLiteral(u: Boolean) = Expr.Literal(ZincChar(previous.lexeme[0]), previous)
	fun numberLiteral(u: Boolean) = Expr.Literal(ZincNumber(parseDouble(previous.lexeme)), previous)
	fun trueLiteral(u: Boolean) = Expr.Literal(ZincTrue, previous)
	fun falseLiteral(u: Boolean) = Expr.Literal(ZincFalse, previous)

	fun or(left: Expr, u: Boolean) = logical(left, Precedence.OR)
	fun and(left: Expr, u: Boolean) = logical(left, Precedence.AND)
	fun equality(left: Expr, u: Boolean) = binary(left, Precedence.COMPARISON)
	fun comparison(left: Expr, u: Boolean) = binary(left, Precedence.TERM)
	fun term(left: Expr, u: Boolean) = binary(left, Precedence.FACTOR)
	fun factor(left: Expr, u: Boolean) = binary(left, Precedence.EXPONENT)
	fun exponent(left: Expr, u: Boolean) = binary(left, Precedence.UNARY)

	fun call(callee: Expr, u: Boolean): Expr.Call? {
		val left = previous
		val arguments = ArrayList<Expr>()
		if (!isNext(RIGHT_PAREN)) {
			do {
				arguments.add(expression() ?: return null)
			} while (match(COMMA))
		}
		expect(RIGHT_PAREN, "Expected ')' after function arguments.") ?: return null
		return Expr.Call(callee, left, arguments.toTypedArray(), previous)
	}

	fun dot(callee: Expr, canAssign: Boolean): Expr? {
		expect(IDENTIFIER, "Expected field name after '.'.") ?: return null
		val name = previous
		if (!canAssign || !match(EQUAL)) return Expr.GetField(callee, name)
		return Expr.SetField(callee, name, expression() ?: return null)
	}

	fun init(callee: Expr, u: Boolean): Expr.InitializeStruct? {
		if (callee !is Expr.GetVariable) {
			error("Invalid struct initialization target.")
			return null
		}
		val fields = ArrayList<Pair<Token, Expr>>()
		if (!isNext(RIGHT_BRACE)) {
			do {
				val pair = getNameAndExpression("field") ?: return null
				fields.add(pair)
			} while (match(COMMA))
		}
		expect(RIGHT_BRACE, "Expected '}' after struct initialization.")
		return Expr.InitializeStruct(callee.variable, fields.toTypedArray(), previous)
	}

	private fun binary(left: Expr, next: Precedence): Expr.Binary? {
		val operator = previous
		val right = parsePrecedence(next) ?: return null
		return Expr.Binary(left, right, operator)
	}

	private fun logical(left: Expr, next: Precedence): Expr.Logical? {
		val operator = previous
		val right = parsePrecedence(next) ?: return null
		return Expr.Logical(left, right, operator)
	}

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

	private fun getNameAndType(variableType: String): Pair<Token, Token>? {
		expect(IDENTIFIER, "Expected $variableType name.") ?: return null
		val name = previous
		expect(COLON, "Expected ':' after $variableType name.") ?: return null
		expect(IDENTIFIER, "Expected $variableType type after ':'.") ?: return null
		return Pair(name, previous)
	}

	private fun getNameAndExpression(variableType: String): Pair<Token, Expr>? {
		expect(IDENTIFIER, "Expected $variableType name.") ?: return null
		val name = previous
		expect(COLON, "Expected ':' after $variableType name.") ?: return null
		val expression = expression() ?: return null
		return Pair(name, expression)
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