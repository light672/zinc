package com.light672.zinc.lang.compiler.ast

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.ast.syntax.*
import com.light672.zinc.lang.compiler.ast.syntax.Token.Type.*
import com.light672.zinc.lang.compiler.ir.Namespace
import com.light672.zinc.lang.tool.Either
import java.lang.Double.parseDouble

// TODO: change this to use value error handling instead of try catch with ParseError

internal class Parser(
	source: String,
	private val moduleName: String,
	private val namespace: Namespace,
	private val runtime: Zinc.Runtime
) {
	companion object {
		fun parse(source: String, moduleName: String, namespace: Namespace, runtime: Zinc.Runtime): Stmt.Module {
			return Parser(source, moduleName, namespace, runtime).parse()
		}
	}

	private fun expressionStmt(expression: Expr): Stmt.Expression {
		var semicolon: Token? = null

		val trailing = when (expression) {
			is Expr.Block -> { // TODO: add if, while, for, loop, to this
				!match(SEMICOLON)
			}

			else -> {
				if (isNext(RIGHT_BRACE)) true else {
					expect(SEMICOLON, "Expected ';' after expression.")
					semicolon = previous
					false
				}
			}
		}

		return Stmt.Expression(expression, trailing, semicolon)
	}

	private fun functionStmt(): Stmt {
		val def = previous
		expect(IDENTIFIER, "Expected function name after 'fn'.")
		val name = previous
		val genericParams = optionalGenericParams()
		expect(LEFT_PAREN, "Expected '(' after ${if (genericParams == null) "function name" else "generic parameters"}.")
		val functionParams = if (match(RIGHT_PAREN)) emptyList() else {
			val params = ArrayList<Stmt.Function.FunctionParam>()
			do {
				params.add(
					when (val pattern = patternOrSelf()) {
						is Either.Right -> Stmt.Function.SelfParam(pattern.value.first, pattern.value.second)
						is Either.Left -> {
							expect(COLON, "Expected ':' and parameter type after parameter name.")
							Stmt.Function.NormalParam(pattern.value, type())
						}
					}
				)
			} while (match(COMMA) && !isNext(RIGHT_PAREN))
			expect(RIGHT_PAREN, "Expected ')' after function parameters.")
			params
		}
		val paramClose = previous
		val returnType = if (match(COLON)) type() else null
		return Stmt.Function(
			def, name, genericParams, functionParams, paramClose, returnType, if (match(SEMICOLON))
				Either.Left(previous)
			else {
				expect(LEFT_BRACE, "Expected block after function declaration")
				Either.Right(block())
			}
		)
	}

	private fun variableStmt(): Stmt {
		val keyword = previous
		val pattern = pattern()
		val type = if (match(COLON)) type() else null
		val initializer = if (match(EQUAL)) expression() else null
		expect(SEMICOLON, "Expected ';' after variable declaration.")
		return Stmt.Variable(keyword, pattern, type, initializer, previous)
	}

	fun block(): Expr.Block {
		val open = previous
		val stmts = ArrayList<Stmt>()
		while (!isNext(RIGHT_BRACE)) {
			try {
				stmts.add(statement())
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
	fun factor(a: Expr) = binary(a, Precedence.UNARY)
	fun unary(): Expr {
		val operator = previous
		val expr = parsePrecedence(Precedence.UNARY)
		return Expr.Unary(expr, operator)
	}

	// </editor-fold>
	// <editor-fold desc="genericParams"
	private fun optionalGenericParams(): GenericParams? {
		if (!match(LESS)) return null
		val open = previous
		val params = ArrayList<TypeParam>()
		if (match(GREATER)) return GenericParams(open, params, previous)
		params.add(expectTypeParam())
		while (match(COMMA)) params.add(optionalTypeParam() ?: break)
		expect(GREATER, "Expected '>' after generic parameters.")
		return GenericParams(open, params, previous)
	}

	// </editor-fold>
	// <editor-fold desc="genericArgs">
	private fun optionalGenericArgs() = if (match(LESS)) genericArgs(previous) else null

	private fun genericArgs(open: Token): GenericArgs {
		if (match(GREATER)) return GenericArgs(open, emptyList(), previous)
		val args = ArrayList<Type>()
		args.add(type())
		while (match(COMMA)) args.add(optionalType() ?: break)
		expect(GREATER, "Expected '>' after generic arguments.")
		return GenericArgs(open, args, previous)
	}

	// </editor-fold>
	// <editor-fold desc="typeParam"
	private fun optionalTypeParam() = if (match(IDENTIFIER)) typeParam(previous) else null

	private fun expectTypeParam(): TypeParam {
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
	private fun optionalTypePath() = if (match(IDENTIFIER, COLON_COLON)) typePath() else null

	private fun expectTypePath(error: String): ComplexPath {
		return if (match(COLON_COLON)) typePath() else {
			expect(IDENTIFIER, error)
			typePath()
		}
	}

	private fun typePath(): ComplexPath {
		fun typePathSegment(): ComplexPath.Segment {
			val begin = previous
			return if (match(LESS)) {
				val a = ComplexPath.Segment(begin, genericArgs(previous))
				match(COLON_COLON)
				a
			} else if (match(COLON_COLON)) {
				val generics = optionalGenericArgs()
				generics?.let { match(COLON_COLON) }
				ComplexPath.Segment(begin, generics)
			} else
				ComplexPath.Segment(begin, null)
		}

		val tail = ArrayList<ComplexPath.Segment>().also {
			if (previous.type == COLON_COLON) {
				it.add(ComplexPath.Segment.none(previous))
				expect(IDENTIFIER, "Expected identifier after '::'.")
			}
		}
		var head = typePathSegment()
		while (previous.type == COLON_COLON) {
			tail.add(head)
			expect(IDENTIFIER, "Expected identifier after '::'.")
			head = typePathSegment()
		}
		return ComplexPath(tail, head)
	}


	// </editor-fold>
	// <editor-fold desc="pathExpression"
	private fun pathExpression(begin: Token): Expr.Path {
		val segments = ArrayList<ComplexPath.Segment>()
		var head = ComplexPath.Segment(
			begin,
			if (match(COLON_COLON)) {
				val generics = optionalGenericArgs()
				match(COLON_COLON)
				generics
			} else null
		)
		while (previous.type == COLON_COLON) {
			segments.add(head)
			head = pathExpressionSegment()
		}
		return Expr.Path(begin, ComplexPath(segments, head), previous)
	}

	fun pathExpressionFromRoot(): Expr.Path {
		val begin = previous
		val segments = arrayListOf(ComplexPath.Segment.none(begin))
		var head = pathExpressionSegment()
		while (previous.type == COLON_COLON) {
			segments.add(head)
			head = pathExpressionSegment()
		}
		return Expr.Path(begin, ComplexPath(segments, head), previous)
	}

	private fun pathExpressionSegment(): ComplexPath.Segment {
		expect(IDENTIFIER, "Expected identifier after '::'.")
		val segment = previous
		val genericArgs =
			if (match(COLON_COLON)) {
				val generics = optionalGenericArgs()
				match(COLON_COLON)
				generics
			} else null
		return ComplexPath.Segment(segment, genericArgs)
	}

	// </editor-fold>
	// <editor-fold desc="typeParamBounds">
	private fun optionalTypeParamBounds(): TypeParamBounds? {
		return if (match(COLON)) {
			expect(IDENTIFIER, "Expected type parameter bounds.")
			TypeParamBounds(typePath())
		} else null
	}

	// </editor-fold>
	// <editor-fold desc="type">
	private fun optionalType() = optionalTypePath()
	private fun type() = expectTypePath("Expected type.")

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

	private fun match(vararg types: Token.Type) =
		if (current.type in types) {
			advance()
			true
		} else false

	private fun end() = isNext(EOF)
	private fun isNext(type: Token.Type) = current.type == type
	private fun error(message: String) = errorAt(previous, message)
	private fun errorAtCurrent(message: String) = errorAt(current, message)
	private fun errorAt(token: Token, message: String): ParseError {
		runtime.reportCompileError("line ${token.line} at '${token.lexeme}' : $message", token..token)
		return ParseError()
	}


	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT)
	private fun declaration(): Stmt {
		if (match(SEMICOLON)) return declaration()
		if (match(FN)) return functionStmt()
		if (match(LET)) return variableStmt()
		throw errorAtCurrent("Expected declaration.")
	}

	private fun statement(): Stmt {
		if (match(SEMICOLON)) return statement()
		if (match(FN)) return functionStmt()
		if (match(LET)) return variableStmt()
		return expressionStmt(expression())
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


	fun parse(): Stmt.Module {
		advance()
		val statements = ArrayList<Stmt>()
		val module = Stmt.Module(Token.empty(), Token.newNA(moduleName, 0, 0..0), statements)
		while (!end()) {
			try {
				val declaration = declaration()
				statements.add(declaration)
				namespace.addItem(declaration)
			} catch (error: ParseError) {
				synchronize()
			}
		}
		return module
	}

	private fun synchronize() {
		advance()
		var braces = 0
		while (!end()) {
			if (previous.type == SEMICOLON) return
			when (current.type) {
				STRUCT, FN, WHILE -> return
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