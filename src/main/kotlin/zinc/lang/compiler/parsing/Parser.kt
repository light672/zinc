package zinc.lang.compiler.parsing

import zinc.Zinc
import zinc.builtin.*
import zinc.lang.compiler.CompilerError
import zinc.lang.compiler.parsing.Token.Type.*
import java.lang.Double.parseDouble

internal class Parser(source: String, private val instance: Zinc.Runtime) {
	private val lexer: Lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()

	private var inFunction = FunctionType.NONE

	enum class FunctionType {
		NONE, UNIT, VALUE
	}

	internal fun parse(): Pair<ArrayList<Statement.Function>, ArrayList<Statement.VariableDeclaration>> {
		advance()
		val functions = ArrayList<Statement.Function>()
		val variables = ArrayList<Statement.VariableDeclaration>()
		while (!end()) {
			val declaration = parseDeclaration()
			if (declaration is Statement.Function) functions.add(declaration)
			else variables.add(declaration as Statement.VariableDeclaration)
		}
		return Pair(functions, variables)
	}


	private fun parseStatement() = declarationOrStatement() ?: throw RuntimeException()
	private fun parseDeclaration() = declaration() ?: throw RuntimeException()

	private fun block(startBracketError: String): Array<Statement>? {
		expect(LEFT_BRACE, startBracketError) ?: return null
		val statements = ArrayList<Statement>()
		if (!isNext(RIGHT_BRACE)) {
			do {
				statements.add(parseStatement())
			} while (!isNext(RIGHT_BRACE))
		}
		expect(RIGHT_BRACE, "Expected '}' after block.") ?: return null
		return statements.toTypedArray()
	}

	private fun declaration(): Statement? {
		// if (match(STRUCT)) return structDeclaration()
		if (match(FUNC)) return functionDeclaration()
		if (match(arrayOf(VAR, VAL))) return variableDeclaration()
		errorAtCurrent("Expected declaration.")
		return null
	}

	private fun declarationOrStatement(): Statement? {
		if (match(FUNC)) return functionDeclaration()
		if (match(arrayOf(VAR, VAL))) return variableDeclaration()
		return statement()
	}

	private fun functionDeclaration(): Statement.Function? {
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
		expect(RIGHT_PAREN, "Expected ')' after function arguments.") ?: return null
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
		return block?.let { Statement.Function(declaration, name, list.toTypedArray(), rightParen, type, it, previous) }
	}

	private fun variableDeclaration(): Statement.VariableDeclaration? {
		val declaration = previous
		expect(IDENTIFIER, "Expected variable name after '${declaration.lexeme}'.") ?: return null
		val name = previous

		var type: Token? = null
		var initializer: Expression? = null

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
		return Statement.VariableDeclaration(declaration, name, type, initializer, previous)
	}

	private fun statement(): Statement? {
		return expressionStatement();
	}

	private fun expressionStatement(): Statement.ExpressionStatement? {
		val expression = expression() ?: return null
		expect(SEMICOLON, "Expected ';' after expression.") ?: return null
		return Statement.ExpressionStatement(expression, previous)
	}

	private fun expression() = assignment()

	private fun assignment(): Expression? {
		val expression = equality() ?: return null
		if (match(EQUAL)) {
			val value = expression() ?: return null
			return when (expression) {
				is Expression.GetVariable -> Expression.SetVariable(expression.variable, value)
				else -> {
					instance.reportCompileError(CompilerError.OneRangeError(expression.getRange(), "Invalid assignment target."))
					null
				}
			}
		}
		return expression
	}

	private fun equality() = parseBinaryExpression({ comparison() }, BANG_EQUAL, EQUAL_EQUAL)
	private fun comparison() = parseBinaryExpression({ modulo() }, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)
	private fun modulo() = parseBinaryExpression({ term() }, PERCENT)
	private fun term() = parseBinaryExpression({ factor() }, MINUS, PLUS)
	private fun factor() = parseBinaryExpression({ unary() }, SLASH, STAR)

	private fun unary(): Expression? {
		if (match(arrayOf(BANG, MINUS))) {
			val operator = previous
			val right = unary() ?: return null
			return Expression.Unary(operator, right)
		}
		return call()
	}

	private fun call(): Expression? {
		val callee = primary() ?: return null
		while (match(LEFT_PAREN)) return finishFunctionCall(callee)
		return callee
	}

	private fun finishFunctionCall(callee: Expression): Expression? {
		val left = previous
		val arguments = ArrayList<Expression>()
		if (!isNext(RIGHT_PAREN)) {
			do {
				arguments.add(expression() ?: return null)
			} while (match(COMMA))
		}
		expect(RIGHT_PAREN, "Expected ')' after function arguments.") ?: return null
		return Expression.Call(callee, left, arguments.toTypedArray(), previous)
	}

	/**
	 * Returns literals and groups.
	 * Errors already handled, returns null if an error was found and processed.
	 */
	private fun primary(): Expression? {
		if (match(FALSE)) return Expression.Literal(ZincFalse, previous)
		if (match(TRUE)) return Expression.Literal(ZincTrue, previous)
		if (match(NUMBER_VALUE)) return Expression.Literal(ZincNumber(parseDouble(previous.lexeme)), previous)
		if (match(STRING_VALUE)) return Expression.Literal(ZincString(previous.lexeme), previous)
		if (match(IDENTIFIER)) return Expression.GetVariable(previous)
		if (match(CHAR_VALUE)) {
			return if (previous.lexeme.length != 1) {
				error(
					if (previous.lexeme.length > 1) "Too many characters in a character literal '${previous.lexeme}'."
					else "Cannot have an empty character literal."
				)
				null
			} else Expression.Literal(ZincChar(previous.lexeme[0]), previous)
		}
		if (match(LEFT_PAREN)) {
			val leftParen = previous
			if (match(RIGHT_PAREN)) return Expression.Unit(leftParen, previous)
			val expression = expression() ?: return null
			expect(RIGHT_PAREN, "Expect ')' after expression.") ?: return null
			return Expression.Grouping(expression, leftParen, previous)
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
			return Expression.Return(returnToken, expression)
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

	private fun getNameAndType(variableType: String): Pair<Token, Token>? {
		expect(IDENTIFIER, "Expected $variableType name.") ?: return null
		val name = previous
		expect(COLON, "Expected ':' after $variableType name.") ?: return null
		expect(IDENTIFIER, "Expected $variableType type after ':'.") ?: return null
		return Pair(name, previous)
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
