package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.parsing.Token.Type.*
import java.lang.Double.parseDouble

internal class PrattParser(source: String, runtime: Zinc.Runtime) : Parser(source, runtime) {
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
		val block = block("Expected function body.")
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

	override fun expression() = parsePrecedence(Precedence.ASSIGNMENT)

	fun unary(u: Boolean): Expr.Unary? {
		return Expr.Unary(previous, parsePrecedence(Precedence.UNARY) ?: return null)
	}

	fun parenthesis(u: Boolean): Expr? {
		val p = previous
		if (match(RIGHT_PAREN)) return Expr.Unit(p, previous)
		val expression = expression() ?: return null
		expect(RIGHT_PAREN, "Expected ')' after expression.") ?: return null
		return Expr.Grouping(expression, p, previous)
	}

	fun returnExpr(u: Boolean): Expr.Return? {
		return if (!isNext(RIGHT_PAREN, SEMICOLON, COMMA, RIGHT_BRACE, RIGHT_BRACKET, ELSE, ELIF))  // only tokens that can come after a return
			Expr.Return(previous, expression() ?: return null)
		else Expr.Return(previous, null)
	}

	fun breakExpr(u: Boolean): Expr? {
		TODO("not yet implemented")
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

	fun or(left: Expr, u: Boolean) = logical(left, Precedence.AND)
	fun and(left: Expr, u: Boolean) = logical(left, Precedence.EQUALITY)
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


}