package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.parsing.syntax.*
import com.light672.zinc.lang.compiler.parsing.syntax.Token.Type.*
import com.light672.zinc.lang.compiler.parsing.syntax.tools.Either
import java.lang.Double.parseDouble

internal class Parser(source: String, private val runtime: Zinc.Runtime) {

	private fun expressionStmt(): Stmt {
		val expression = expression()
		expect(SEMICOLON, "Expected ';' after statement.")
		return Stmt.Expression(expression)
	}

	private fun functionStmt(): Stmt {
		val def = previous
		val genericParams = optionalGenericParams()
		expect(IDENTIFIER, "Expected function name after 'def'.")
		val name = previous
		fun functionParameters(): Stmt.Function.FunctionParams? {
			expect(LEFT_PAREN, "Expected '(' after function name.")
			val params = ArrayList<Stmt.Function.FunctionParams.FunctionParam>()
			if (match(RIGHT_PAREN)) return null
			do {
				if (isNext(RIGHT_PAREN)) break
				if (match(MUT) || isNext(SELF)) {
					val mut = if (previous.type == MUT) previous else null
					expect(SELF, "Expected 'self' after 'mut' in method parameters.")
					params.add(Stmt.Function.FunctionParams.FunctionParam.SelfParam(mut, previous))
				} else {
					expect(IDENTIFIER, "Expected parameter name.")
					val name = previous
					expect(COLON, "Expected ':' and parameter type after parameter name.")
					val type = type()
					params.add(Stmt.Function.FunctionParams.FunctionParam.NormalParam(name, type))
				}
			} while (match(COMMA))
			expect(RIGHT_PAREN, "Expected ')' after function parameters.")
			return Stmt.Function.FunctionParams(params)
		}

		val functionParams = functionParameters()
		val returnType = if (match(COLON)) Stmt.Function.ReturnType(previous, type()) else null
		expect(SEMICOLON, "Expected ';' after function declaration") // temporary
		return Stmt.Function(def, genericParams, name, functionParams, returnType, Either.Left(previous))
	}

	private fun variableStmt(): Stmt {
		if (!isNext(COLON, COLON_EQUAL)) {
			val v = Stmt.Expression(variable())
			expect(SEMICOLON, "Expected ';' after statement.")
			return v
		}
		val name = previous
		var type = if (match(COLON)) type() else null
		var expr = if (type == null) {
			advance()
			expression()
		} else if (match(EQUAL)) expression() else null

		expect(SEMICOLON, "Expected ';' after variable declaration.")
		return Stmt.Variable(name, type, expr)
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

	fun variable() = Expr.Variable(previous)


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

	// <editor-fold desc="genericParams()"
	private fun optionalGenericParams() = if (match(LESS)) genericParams(previous) else null

	private fun genericParams(): GenericParams {
		expect(LESS, "Expected generic parameters.")
		return genericParams(previous)
	}

	private fun genericParams(open: Token): GenericParams {
		val params = ArrayList<TypeParam>()
		if (isNext(GREATER)) throw errorAtCurrent("Generic parameters must have at least one parameter.")
		params.add(typeParam())
		while (match(COMMA) && !isNext(GREATER)) {
			params.add(typeParam())
		}
		expect(GREATER, "Expected '>' after generic parameters.")
		return GenericParams(open, params, previous)
	}

	// </editor-fold>
	// <editor-fold desc="genericArgs()">
	private fun optionalGenericArgs() = if (match(LESS)) genericArgs(previous) else null

	private fun genericArgs(): GenericArgs {
		expect(LESS, "Expected generic args.")
		return genericArgs(previous)
	}

	private fun genericArgs(open: Token): GenericArgs {
		val args = ArrayList<Type>()
		if (isNext(GREATER)) throw errorAtCurrent("Generic arguments must have at least one argument.")
		args.add(type())
		while (match(COMMA) && !isNext(GREATER)) {
			args.add(type())
		}
		expect(GREATER, "Expected '>' after generic arguments.")
		return GenericArgs(open, args, previous)
	}

	// </editor-fold>
	// <editor-fold desc="typeParam()"
	private fun optionalTypeParam() = if (match(IDENTIFIER)) typeParam(previous) else null

	private fun typeParam(): TypeParam {
		expect(IDENTIFIER, "Expected type parameter.")
		return typeParam(previous)
	}

	private fun typeParam(name: Token): TypeParam {
		val bounds = optionalTypeParamBounds()
		val type = if (match(EQUAL)) type() else null
		return TypeParam(name, bounds, type)
	}

	// </editor-fold>
	// <editor-fold desc="typePath()">
	private fun optionalTypePath() = if (match(IDENTIFIER)) typePath(previous) else null

	private fun typePath(error: String): TypePath {
		expect(IDENTIFIER, error)
		return typePath(previous)
	}

	private fun typePath(tailName: Token): TypePath {
		var enterLoop = false
		val tail: TypePath.TypePathSegment =
			if (match(COLON_COLON) || isNext(LESS)) {
				if (!isNext(IDENTIFIER)) {
					expect(LESS, "Expected identifier or generic arguments after '::' in type path.")
					val genericArgs = genericArgs(previous)
					TypePath.TypePathSegment(tailName, genericArgs)
				} else {
					enterLoop = true
					TypePath.TypePathSegment(tailName, null)
				}
			} else TypePath.TypePathSegment(tailName, null)

		val body = ArrayList<TypePath.TypePathSegment>()
		enterLoop = enterLoop || match(COLON_COLON)
		while (enterLoop) {
			expect(IDENTIFIER, "Expected identifier after '::'.")
			val segmentName = previous
			if (match(COLON_COLON) || isNext(LESS)) {
				if (isNext(IDENTIFIER)) continue
				expect(LESS, "Expected identifier or generic arguments after '::' in type path.")
				val genericArgs = genericArgs(previous)
				body.add(TypePath.TypePathSegment(segmentName, genericArgs))
			} else {
				body.add(TypePath.TypePathSegment(segmentName, null))
			}
			enterLoop = match(COLON_COLON)
		}

		return TypePath(tail, body)
	}

	// </editor-fold>
	// <editor-fold desc="typeParamBounds()">
	private fun optionalTypeParamBounds() = if (match(COLON)) typeParamBounds() else null

	private fun typeParamBounds(): TypeParamBounds {
		expect(IDENTIFIER, "Expected type parameter bounds.")
		return typeParamBounds(previous)
	}

	private fun typeParamBounds(identifier: Token): TypeParamBounds {
		return TypeParamBounds(typePath(identifier))
	}

	// </editor-fold>

	private fun type() = typePath("Expected type.")


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
		runtime.reportCompileError("line ${token.line} at '${token.lexeme}' : $message")
		return ParseError()
	}

	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT)
	private fun declaration() = statement()
	private fun statement(): Stmt {
		if (match(IDENTIFIER)) return variableStmt()
		if (match(DEF)) return functionStmt()
		return expressionStmt()
	}

	private fun parsePrecedence(precedence: Precedence): Expr {
		advance()
		val rule = previous.type.rule.prefix ?: throw error("Expected expression.")
		// val canAssign = precedence.ordinal <= Precedence.ASSIGNMENT.ordinal
		var left = rule()
		while (precedence.ordinal <= current.type.rule.precedence.ordinal) {
			advance()
			val infix = previous.type.rule.infix!!
			left = infix(left)
		}
		// if (canAssign && match(EQUAL)) throw error("Invalid assignment target.")
		return left
	}


	fun parse() {
		val statements = ArrayList<Stmt>()
		advance()
		while (!end()) {
			/* try {
				statements.add(declaration())
			} catch (error: ParseError) {
			}*/
			statements.add(declaration())
		}
	}

	class ParseError : RuntimeException()
}