package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.Token.Type.*
import com.light672.zinc.lang.runtime.opcodes.OP_ADD
import com.light672.zinc.lang.runtime.opcodes.OP_DIV
import com.light672.zinc.lang.runtime.opcodes.OP_POW
import com.light672.zinc.lang.runtime.opcodes.OP_SUB
import java.lang.Double.parseDouble

internal class Compiler(source: String, val runtime: Zinc.Runtime) {
	var scopeDepth = 0
	val scopes = arrayOfNulls<Scope>(256)
	fun compile() {
		advance()
		while (!end()) declaration() ?: synchronize()
	}

	private fun declaration(): Unit? {
		if (match(STRUCT)) return structDeclaration()
		if (match(DEF)) return functionDeclaration()
		if (match(arrayOf(VAR, VAL))) return variableDeclaration()
		if (match(IMPL)) return implStatement()
		return statement()
	}

	private fun statement(): Unit? {
		return when (current.type) {
			else -> expressionStatement()
		}
	}

	private fun expressionStatement(): Unit? {
		val expression = expression() ?: return null
		expect(SEMICOLON, "Expected ';' after expression.") ?: return null
		return Unit
	}

	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT)

	fun unary(u: Boolean): ExprData? {
		val operator = previous
		val expression = parsePrecedence(Precedence.UNARY) ?: return null
		val typeExpected = when (operator.type) {
			MINUS -> Type.Number
			BANG -> Type.Bool
			else -> throw IllegalArgumentException()
		}
		if (expression.type != typeExpected) return rangeError(expression.range, "Expected type '$typeExpected' but got '${expression.type}'.")
		// generate code
		expression.range = operator.range.first..previous.range.last
		return expression
	}

	fun parenthesis(u: Boolean): ExprData? {
		val p = previous
		if (match(RIGHT_PAREN)) return ExprData(Type.Unit, p.range.first..p.range.last)
		val expression = expression() ?: return null
		expect(RIGHT_PAREN, "Expected ')' after expression.") ?: return null
		expression.range = p.range.first..previous.range.last
		return expression
	}

	fun returnExpr(u: Boolean): ExprData? {
		val keyword = previous
		if (!isNext(RIGHT_PAREN, SEMICOLON, COMMA, RIGHT_BRACE, RIGHT_BRACKET, ELSE, ELIF)) // only tokens that can come after a return
			expression() ?: return null

		// generate code

		return ExprData(Type.Never, keyword.range.first..previous.range.last)
	}

	fun breakExpr(u: Boolean): ExprData? {
		TODO("not yet implemented")
	}


	fun variable(canAssign: Boolean): ExprData? {

		TODO("not yet implemented")
		/*
		val name = previous
		if (!canAssign || !match(EQUAL)) return Expr.GetVariable(name)
		return Expr.SetVariable(name, expression() ?: return null)
		*/
	}


	fun stringLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.String, previous.range)
	}

	fun charLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.Char, previous.range)
	}

	fun numberLiteral(u: Boolean): ExprData {
		parseDouble(previous.lexeme)
		// generate code
		return ExprData(Type.Number, previous.range)
	}

	fun trueLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.Bool, previous.range)
	}

	fun falseLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.Bool, previous.range)
	}

	fun or(left: ExprData, u: Boolean) = binary(left, Precedence.AND, OP_OR, Type.Number)
	fun and(left: ExprData, u: Boolean) = binary(left, Precedence.EQUALITY, OP_AND, Type.Number)
	fun equality(left: ExprData, u: Boolean) = binary(left, Precedence.COMPARISON, OP_EQUAL, Type.Number)
	fun comparison(left: ExprData, u: Boolean): ExprData? {
		val operator = previous
		val right = parsePrecedence(Precedence.TERM) ?: return null
		val range = left.range.first..right.range.last
		if (!(left.type == Type.Number && right.type == Type.Number)) {
			return rangeError(range, "Cannot perform binary '${operator.lexeme}' on types '${left.type}' and '${right.type}'.")
		}
		left.range = range
		left.type = Type.Bool
		// generate code
		return left
	}

	fun term(left: ExprData, u: Boolean) = binary(left, Precedence.FACTOR, if (previous.type == PLUS) OP_ADD else OP_SUB, Type.Number)
	fun factor(left: ExprData, u: Boolean) = binary(left, Precedence.EXPONENT, if (previous.type == STAR) OP_ADD else OP_DIV, Type.Number)
	fun exponent(left: ExprData, u: Boolean) = binary(left, Precedence.UNARY, OP_POW, Type.Number)

	fun call(callee: ExprData, u: Boolean): ExprData? {
		if (callee.type !is Type.Function) return rangeError(callee.range, "Cannot perform call on type '${callee.type}'.")

		var arity = 0
		val argArity = (callee.type as Type.Function).args.size - 1
		if (!isNext(RIGHT_PAREN)) {
			do {
				if (arity > argArity) {
					expression() ?: return null
					continue
				}

				val expression = expression() ?: return null
				val argType = (callee.type as Type.Function).args[arity++]
				if (argType != expression.type) return rangeError(callee.range, "Expected type '${expression.type}' but got '${argType}'.")
			} while (match(COMMA))

			if (arity != argArity) return rangeError(
				callee.range.first..previous.range.last,
				"Function has '$argArity' argument${if (argArity > 1) "s" else ""} but '${arity}' ${if (arity > 1) "were" else "was"} given."
			)
		}
		expect(RIGHT_PAREN, "Expected ')' after function arguments.") ?: return null
		callee.range = callee.range.first..previous.range.last
		callee.type = (callee.type as Type.Function).returnType
		return callee
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

	private fun binary(left: ExprData, next: Precedence, opcode: Byte, type: Type): ExprData? {
		val operator = previous
		val right = parsePrecedence(next) ?: return null
		val range = left.range.first..right.range.last
		if (!(left.type == type && right.type == type)) {
			return rangeError(range, "Cannot perform '${operator.lexeme}' on types '${left.type}' and '${right.type}'.")
		}
		left.range = range
		// generate code
		return left
	}

	private fun parsePrecedence(precedence: Precedence): ExprData? {
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


	private val lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()

	private fun advance(): Unit? {
		previous = current; current = lexer.scanToken(); return if (current.type == Token.Type.ERROR) {
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

	private fun end() = current.type == Token.Type.EOF
	private fun isNext(type: Token.Type) = current.type == type
	private fun isNext(vararg types: Token.Type) = current.type in types
	private fun error(message: String) = errorAt(previous, message)
	private fun errorAtCurrent(message: String) = errorAt(current, message)
	private fun errorAt(token: Token, message: String) = runtime.reportCompileError(message)
}