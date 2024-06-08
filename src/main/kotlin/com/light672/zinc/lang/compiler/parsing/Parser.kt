package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.parsing.syntax.*
import com.light672.zinc.lang.compiler.parsing.syntax.Token.Type.*
import com.light672.zinc.lang.compiler.parsing.syntax.tools.Either
import java.lang.Double.parseDouble

internal class Parser(source: String, private val runtime: Zinc.Runtime) {

	private fun expressionStmt(expression: Expr): Stmt.Expression {
		fun doWhen(expression: Expr): Boolean {
			return when (expression) {
				is Expr.MutableReference -> {
					doWhen(expression.expr)
				}

				is Expr.WithoutBlock -> {
					if (isNext(RIGHT_BRACE)) true else {
						expect(SEMICOLON, "Expected ';' after statement.")
						false
					}
				}

				is Expr.WithBlock -> true
				else -> {
					TODO("forgot to attach WithoutBlock or WithBlock to ${expression.javaClass}")
				}
			}
		}

		val trailing = doWhen(expression)
		return Stmt.Expression(expression, trailing)
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

				val pattern = patternOrSelf()
				if (pattern is Either.Right<Pattern, Pair<Token?, Token>>) {
					params.add(Stmt.Function.FunctionParams.FunctionParam.SelfParam(pattern.value.first, pattern.value.second))
					continue
				}
				pattern as Either.Left<Pattern, Pair<Token?, Token>>
				expect(COLON, "Expected ':' and parameter type after parameter name.")
				val type = type()
				params.add(Stmt.Function.FunctionParams.FunctionParam.NormalParam(pattern.value, type))
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
		val expr = expression()
		if (!isNext(COLON, COLON_EQUAL)) return expressionStmt(expr)
		val pattern = patternFrom(expr)
		val type = if (match(COLON)) type() else null
		val initializer = if (type == null) {
			advance()
			expression()
		} else if (match(EQUAL)) expression() else null

		expect(SEMICOLON, "Expected ';' after variable declaration.")
		return Stmt.Variable(pattern, type, initializer)
	}

	fun block(): Expr.Block {
		val open = previous
		val stmts = ArrayList<Stmt>()
		while (!isNext(RIGHT_BRACE)) {
			try {
				stmts.add(declaration())
			} catch (error: ParseError) {
				synchronize()
			}
		}
		expect(RIGHT_BRACE, "Expected '}' to close block.")
		return Expr.Block(open, stmts, previous)
	}

	fun charLiteral() = Expr.Literal(ZincChar(previous.lexeme[0]), previous)
	fun stringLiteral() = Expr.Literal(ZincString(previous.lexeme), previous)
	fun numberLiteral() = Expr.Literal(ZincNumber(parseDouble(previous.lexeme)), previous)
	fun trueLiteral() = Expr.Literal(ZincTrue, previous)
	fun falseLiteral() = Expr.Literal(ZincFalse, previous)


	fun parenthesis(): Expr {
		val open = previous
		val expr = expression()
		expect(RIGHT_PAREN, "Expected ')' after expression.")
		return Expr.Group(open, expr, previous)
	}

	fun variable() = pathExpression(previous)
	fun mutReference() = Expr.MutableReference(previous, expression())


	// <editor-fold desc="binary">
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

	// </editor-fold>
	// <editor-fold desc="genericParams"
	private fun optionalGenericParams() = if (match(LESS)) genericParams(previous) else null

	private fun genericParams(): GenericParams {
		expect(LESS, "Expected generic parameters.")
		return genericParams(previous)
	}

	private fun genericParams(open: Token): GenericParams {
		val params = ArrayList<TypeParam>()
		if (match(GREATER)) return GenericParams(open, params, previous)
		params.add(typeParam())
		while (match(COMMA)) params.add(optionalTypeParam() ?: break)
		expect(GREATER, "Expected '>' after generic parameters.")
		return GenericParams(open, params, previous)
	}

	// </editor-fold>
	// <editor-fold desc="genericArgs">
	private fun optionalGenericArgs() = if (match(LESS)) genericArgs(previous) else null

	private fun genericArgs(): GenericArgs {
		expect(LESS, "Expected generic args.")
		return genericArgs(previous)
	}

	private fun genericArgs(open: Token): GenericArgs {
		val args = ArrayList<Type>()
		if (match(GREATER)) return GenericArgs(open, args, previous)
		args.add(type())
		while (match(COMMA)) args.add(optionalType() ?: break)
		expect(GREATER, "Expected '>' after generic arguments.")
		return GenericArgs(open, args, previous)
	}

	// </editor-fold>
	// <editor-fold desc="typeParam"
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
	// <editor-fold desc="typePath">
	private fun optionalTypePath() = if (match(IDENTIFIER)) typePath(previous) else if (match(COLON_COLON)) typePath() else null

	private fun typePath(error: String): TypePath {
		return if (match(COLON_COLON)) typePath() else {
			expect(IDENTIFIER, error)
			typePath(previous)
		}
	}

	/**
	 * Called after IDENTIFIER starts a path
	 */
	private fun typePath(tailName: Token): TypePath {
		val tail = ArrayList<TypePath.TypePathSegment>()
		var head = typePathSegment(tailName)
		while (previous.type == COLON_COLON) {
			tail.add(head)
			head = typePathSegment()
		}
		return TypePath(tail, head)
	}

	/**
	 * Called after '::' starts a path.
	 */
	private fun typePath(): TypePath {
		val tail = ArrayList<TypePath.TypePathSegment>().also { it.add(TypePath.TypePathSegment.NONE) }
		expect(IDENTIFIER, "Expected identifier after '::'.")
		var head = typePathSegment(previous)
		while (match(COLON_COLON)) {
			tail.add(head)
			head = typePathSegment()
		}
		return TypePath(tail, head)
	}

	/**
	 * Called after IDENTIFIER
	 * Consumes '::'
	 */
	private fun typePathSegment(begin: Token): TypePath.TypePathSegment {
		return if (match(LESS)) {
			val a = TypePath.TypePathSegment(begin, genericArgs(previous))
			match(COLON_COLON)
			a
		} else if (match(COLON_COLON)) {
			val generics = optionalGenericArgs()
			generics?.let { match(COLON_COLON) }
			TypePath.TypePathSegment(begin, generics)
		} else
			TypePath.TypePathSegment(begin, null)
	}

	/**
	 * Called after '::'
	 */
	private fun typePathSegment(): TypePath.TypePathSegment {
		expect(IDENTIFIER, "Expected identifier after '::'.")
		return typePathSegment(previous)
	}


	// </editor-fold>
	// <editor-fold desc="pathExpression"
	private fun pathExpression(begin: Token): Expr.Path {
		val segments = ArrayList<PathExprSegment>()
		var head = PathExprSegment(
			begin, if (match(COLON_COLON)) {
				val generics = optionalGenericArgs()
				match(COLON_COLON)
				generics
			} else null
		)
		while (previous.type == COLON_COLON) {
			segments.add(head)
			head = pathExpressionSegment()
		}
		return Expr.Path(begin, segments, head, previous)
	}

	fun pathExpression(): Expr.Path {
		val begin = previous
		val segments = ArrayList<PathExprSegment>().also { it.add(PathExprSegment.NONE) }
		var head = pathExpressionSegment()
		while (previous.type == COLON_COLON) {
			segments.add(head)
			head = pathExpressionSegment()
		}
		return Expr.Path(begin, segments, head, previous)
	}

	private fun pathExpressionSegment(): PathExprSegment {
		expect(IDENTIFIER, "Expected identifier after '::'.")
		val segment = previous
		val genericArgs =
			if (match(COLON_COLON)) {
				val generics = optionalGenericArgs()
				match(COLON_COLON)
				generics
			} else null
		return PathExprSegment(segment, genericArgs)
	}

	// </editor-fold>
	// <editor-fold desc="typeParamBounds">
	private

	fun optionalTypeParamBounds() = if (match(COLON)) typeParamBounds() else null

	private fun typeParamBounds(): TypeParamBounds {
		expect(IDENTIFIER, "Expected type parameter bounds.")
		return typeParamBounds(previous)
	}

	private fun typeParamBounds(identifier: Token): TypeParamBounds {
		return TypeParamBounds(typePath(identifier))
	}

	// </editor-fold>
	// <editor-fold desc="type">
	private fun optionalType() = optionalTypePath()
	private fun type() = typePath("Expected type.")

	// </editor-fold>
	// <editor-fold desc="pattern">
	private fun pattern(): Pattern {
		return when (current.type) {
			IDENTIFIER -> {
				advance()
				if (match(COLON_COLON)) {
					Pattern.PathPattern(pathExpression(previous))
				} else
					Pattern.IdentifierPattern(null, previous)
			}

			MUT -> {
				advance()
				val mut = previous
				expect(IDENTIFIER, "Expected identifier after 'mut' in pattern.")
				Pattern.IdentifierPattern(mut, previous)
			}

			else -> throw errorAtCurrent("Expected pattern.")
		}
	}

	private fun patternOrSelf(): Either<Pattern, Pair<Token?, Token>> {
		if (match(SELF)) return Either.Right(Pair(null, previous))
		if (match(MUT)) {
			val mut = previous
			if (match(SELF)) return Either.Right(Pair(mut, previous))
			expect(IDENTIFIER, "Expected identifier or 'self' after 'mut' in pattern for function parameters.")
			return Either.Left(Pattern.IdentifierPattern(mut, previous))
		}
		return Either.Left(pattern())
	}

	private fun patternFrom(expr: Expr): Pattern {
		return when (expr) {
			is Expr.Path -> {
				if (expr.body.size > 0 || expr.head.genericArgs != null)
					Pattern.PathPattern(expr)
				else
					Pattern.IdentifierPattern(null, expr.head.segment)
			}

			is Expr.MutableReference -> {
				if (expr.expr !is Expr.Path) throw exprError(expr, "Expected identifier after 'mut' in pattern.")
				if (expr.expr.body.size > 0 || expr.expr.head.genericArgs != null)
					throw exprError(expr, "Expected identifier after 'mut' in pattern.")
				return Pattern.IdentifierPattern(expr.mut, expr.expr.head.segment)
			}

			else -> throw exprError(expr, "Expected pattern.")
		}
	}

	// </editor-fold>

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
		runtime.reportCompileError("line ${token.line} at '${token.lexeme}' : $message", token..token)
		return ParseError()
	}

	private fun exprError(expr: Expr, message: String): ParseError {
		runtime.reportCompileError("line ${expr.firstToken.line} at '${expr.firstToken.lexeme}' : $message'", expr.range())
		return ParseError()
	}

	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT)
	private fun declaration() = statement()
	private fun statement(): Stmt {
		if (match(SEMICOLON)) return Stmt.Semicolon(previous)
		if (match(DEF)) return functionStmt()
		return variableStmt()
	}

	private fun parsePrecedence(precedence: Precedence): Expr {
		advance()
		val rule = previous.type.rule.prefix ?: run {
			when (previous.type) {
				RIGHT_PAREN, RIGHT_BRACE -> throw error("Unexpected closing delimiter '${previous.lexeme}'. Did you mean: ';'?")
				else -> throw error("Expected expression.")
			}
		}
		var left = rule()
		while (precedence.ordinal <= current.type.rule.precedence.ordinal) {
			advance()
			val infix = previous.type.rule.infix!!
			left = infix(left)
		}
		return left
	}


	fun parse() {
		val statements = ArrayList<Stmt>()
		advance()
		while (!end()) {
			try {
				statements.add(declaration())
			} catch (error: ParseError) {
				synchronize()
			}
		}
	}

	private fun synchronize() {
		advance()
		var braces = 0
		while (!end()) {
			if (previous.type == SEMICOLON) return
			when (current.type) {
				STRUCT, DEF, WHILE -> return
				LEFT_BRACE -> braces++
				RIGHT_BRACE -> {
					if (braces != 0) braces--
					else return
				}

				else -> {}
			}
			advance()
		}
	}

	class ParseError : RuntimeException()
}