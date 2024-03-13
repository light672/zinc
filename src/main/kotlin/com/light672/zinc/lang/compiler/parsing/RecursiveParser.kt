package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.CompilerError
import com.light672.zinc.lang.compiler.parsing.Token.Type.*
import java.lang.Double.parseDouble

internal class RecursiveParser(source: String, private val instance: com.light672.zinc.Zinc.Runtime) {
	private val lexer: Lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()

	private var inFunction = FunctionType.NONE

	enum class FunctionType {
		NONE, UNIT, VALUE
	}

	internal fun parse(): Triple<ArrayList<Stmt.Struct>, ArrayList<Stmt.Function>, ArrayList<Stmt.VariableDeclaration>> {
		advance()
		val functions = ArrayList<Stmt.Function>()
		val variables = ArrayList<Stmt.VariableDeclaration>()
		val structs = ArrayList<Stmt.Struct>()
		while (!end()) {
			when (val declaration = parseDeclaration()) {
				is Stmt.Function -> functions.add(declaration)
				is Stmt.VariableDeclaration -> variables.add(declaration)
				is Stmt.Struct -> structs.add(declaration)
				else -> throw IllegalArgumentException()
			}
		}
		return Triple(structs, functions, variables)
	}


	private fun parseStatement() = declarationOrStatement() ?: throw RuntimeException()
	private fun parseDeclaration() = declaration() ?: throw RuntimeException()

	private fun block(startBracketError: String): Array<Stmt>? {
		expect(LEFT_BRACE, startBracketError) ?: return null
		val statements = ArrayList<Stmt>()
		if (!isNext(RIGHT_BRACE)) {
			do {
				statements.add(parseStatement())
			} while (!isNext(RIGHT_BRACE))
		}
		expect(RIGHT_BRACE, "Expected '}' after block.") ?: return null
		return statements.toTypedArray()
	}

	private fun declaration(): Stmt? {
		if (match(STRUCT)) return structDeclaration()
		if (match(FUNC)) return functionDeclaration()
		if (match(arrayOf(VAR, VAL))) return variableDeclaration()
		errorAtCurrent("Expected declaration.")
		return null
	}

	private fun declarationOrStatement(): Stmt? {
		if (match(FUNC)) return functionDeclaration()
		if (match(arrayOf(VAR, VAL))) return variableDeclaration()
		return statement()
	}

	private fun structDeclaration(): Stmt.Struct? {
		val declaration = previous
		expect(IDENTIFIER, "Expected struct name after 'struct'.") ?: return null
		val name = previous
		expect(LEFT_BRACE, "Expected '{' after 'struct'.")
		val list = ArrayList<Pair<Token, Token>>()
		if (!isNext(RIGHT_BRACE)) {
			do {
				val pair = getNameAndType("field") ?: return null
				list.add(pair)
			} while (match(COMMA))
		}
		expect(RIGHT_BRACE, "Expected '}' after struct fields.") ?: return null
		return Stmt.Struct(declaration, name, list.toTypedArray(), previous)
	}

	private fun functionDeclaration(): Stmt.Function? {
		val declaration = previous
		expect(IDENTIFIER, "Expected function name after 'func'.") ?: return null
		val name = previous
		expect(LEFT_PAREN, "Expected '(' after function name.") ?: return null
		val list = ArrayList<Pair<Token, Token>>()
		if (!isNext(RIGHT_PAREN)) {
			do {
				val pair = getNameAndType("parameter") ?: return null
				list.add(pair)
			} while (match(COMMA))
		}
		expect(RIGHT_PAREN, "Expected ')' after function parameters.") ?: return null
		val rightParen = previous
		var type: Token? = null
		if (match(COLON)) {
			expect(IDENTIFIER, "Expected function return type after ':'.") ?: return null
			type = previous
		}
		val wasInFunction = inFunction
		inFunction = type?.let { FunctionType.VALUE } ?: FunctionType.UNIT
		val block = block("Expected function body.")
		inFunction = wasInFunction
		return block?.let { Stmt.Function(declaration, name, list.toTypedArray(), rightParen, type, it, previous) }
	}

	private fun variableDeclaration(): Stmt.VariableDeclaration? {
		val declaration = previous
		expect(IDENTIFIER, "Expected variable name after '${declaration.lexeme}'.") ?: return null
		val name = previous

		var type: Token? = null
		var initializer: Expr? = null

		if (match(COLON)) {
			expect(IDENTIFIER, "Expected variable type after ':'.") ?: return null
			type = previous
		}
		if (match(EQUAL)) {
			initializer = expression() ?: return null
		}
		if (initializer == null && type == null) {
			error("Variable '${name.lexeme}' must have either a type or an initializer.")
			return null
		}
		expect(SEMICOLON, "Expected ';' after variable declaration.") ?: return null
		return Stmt.VariableDeclaration(declaration, name, type, initializer, previous)
	}

	private fun statement(): Stmt? {
		return expressionStatement();
	}

	private fun expressionStatement(): Stmt.ExpressionStatement? {
		val expression = expression() ?: return null
		expect(SEMICOLON, "Expected ';' after expression.") ?: return null
		return Stmt.ExpressionStatement(expression, previous)
	}

	private fun expression() = assignment()

	private fun assignment(): Expr? {
		val expression = or() ?: return null
		if (match(EQUAL)) {
			val value = expression() ?: return null
			return when (expression) {
				is Expr.GetVariable -> Expr.SetVariable(expression.variable, value)
				is Expr.GetField -> Expr.SetField(expression.obj, expression.field, value)
				else -> {
					instance.reportCompileError(CompilerError.OneRangeError(expression.getRange(), "Invalid assignment target."))
					null
				}
			}
		}
		return expression
	}

	private fun or() = parseLogicalExpression({ and() }, OR)
	private fun and() = parseLogicalExpression({ equality() }, AND)
	private fun equality() = parseBinaryExpression({ comparison() }, BANG_EQUAL, EQUAL_EQUAL)
	private fun comparison() = parseBinaryExpression({ term() }, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)
	private fun term() = parseBinaryExpression({ factor() }, MINUS, PLUS)
	private fun factor() = parseBinaryExpression({ exponent() }, SLASH, STAR, PERCENT)
	private fun exponent() = parseBinaryExpression({ unary() }, CARET)

	private fun unary(): Expr? {
		if (match(arrayOf(BANG, MINUS))) {
			val operator = previous
			val right = unary() ?: return null
			return Expr.Unary(operator, right)
		}
		return call()
	}

	private fun call(): Expr? {
		val callee = primary() ?: return null
		while (true) {
			if (match(LEFT_PAREN))
				return finishFunctionCall(callee)
			if (match(DOT))
				return finishFieldGet(callee)
			break
		}
		return callee
	}

	private fun finishFunctionCall(callee: Expr): Expr? {
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

	private fun finishFieldGet(callee: Expr): Expr? {
		expect(IDENTIFIER, "Expected field name after '.'.") ?: return null
		return Expr.GetField(callee, previous)
	}

	/**
	 * Returns literals and groups.
	 * Errors already handled, returns null if an error was found and processed.
	 */
	private fun primary(): Expr? {
		if (match(FALSE)) return Expr.Literal(ZincFalse, previous)
		if (match(TRUE)) return Expr.Literal(ZincTrue, previous)
		if (match(NUMBER_VALUE)) return Expr.Literal(ZincNumber(parseDouble(previous.lexeme)), previous)
		if (match(STRING_VALUE)) return Expr.Literal(ZincString(previous.lexeme), previous)
		if (match(IDENTIFIER)) {
			val name = previous
			if (!match(LEFT_BRACE)) return Expr.GetVariable(name)
			val fields = ArrayList<Pair<Token, Expr>>()
			if (!isNext(RIGHT_BRACE)) {
				do {
					val pair = getNameAndExpression("field") ?: return null
					fields.add(pair)
				} while (match(COMMA))
			}
			expect(RIGHT_BRACE, "Expected '}' after struct initialization.")
			return Expr.InitializeStruct(name, fields.toTypedArray(), previous)
		}
		if (match(CHAR_VALUE)) {
			return if (previous.lexeme.length != 1) {
				error(
					if (previous.lexeme.length > 1) "Too many characters in a character literal '${previous.lexeme}'."
					else "Cannot have an empty character literal."
				)
				null
			} else Expr.Literal(ZincChar(previous.lexeme[0]), previous)
		}
		if (match(LEFT_PAREN)) {
			val leftParen = previous
			if (match(RIGHT_PAREN)) return Expr.Unit(leftParen, previous)
			val expression = expression() ?: return null
			expect(RIGHT_PAREN, "Expect ')' after expression.") ?: return null
			return Expr.Grouping(expression, leftParen, previous)
		}
		if (match(RETURN)) {
			val returnToken = previous
			val expression = when (inFunction) {
				FunctionType.NONE -> {
					error("Cannot return from top level code.")
					return null
				}

				FunctionType.UNIT -> null
				FunctionType.VALUE -> expression() ?: return null
			}
			return Expr.Return(returnToken, expression)
		}
		errorAtCurrent("Expected expression.")
		return null
	}

	private fun parseBinaryExpression(next: () -> Expr?, vararg types: Token.Type): Expr? {
		var expression = next() ?: return null
		while (match(types)) {
			val operator = previous
			val right = next() ?: return null
			expression = Expr.Binary(expression, right, operator)
		}
		return expression
	}

	private fun parseLogicalExpression(next: () -> Expr?, type: Token.Type): Expr? {
		var expression = next() ?: return null
		while (match(type)) {
			val operator = previous
			val right = next() ?: return null
			expression = Expr.Binary(expression, right, operator)
		}
		return expression
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
		instance.reportCompileError(CompilerError.TokenError(token, message))
	}
}
