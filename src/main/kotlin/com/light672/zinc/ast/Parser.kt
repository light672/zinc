package com.light672.zinc.ast

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.TokenType.*

internal class Parser(val zinc: Zinc.Runtime) {
	val combinator = CombinatorParser(zinc)

	fun declaration(): ParseResult<Stmt> = with(combinator) {
		(function() or ::struct or ::module).error { CompilerError.expectedStatement(current) }
	}

	fun function(): ParseResult<Stmt> = with(combinator) {
		val keyword = token(FN)
		val name = keyword
			.then { expect(token(IDENTIFIER)) }
		val genericParams = name
			.then { optional(genericParams()) }
		val params = genericParams.then {
			expect(run {
				val open = token(LEFT_PAREN)
				open
					.then { expect(manyTrailingUntil(::functionParam, COMMA, RIGHT_PAREN)) }
					.map { (list, close) -> Triple(open, list, close) }
			})
		}
		val returnType = params.then { optional(token(COLON).then { expect(type()) }) }

		val functionNoBlock =
			token(SEMICOLON).map { token -> Stmt.FunctionNoBlock(+keyword, +name, +genericParams, (+params).second, +returnType, token) }
		val functionWithBlockParser =
			{ block().map { block -> Stmt.Function(+keyword, +name, +genericParams, (+params).second, +returnType, block) } }

		returnType
			.then { expect(functionNoBlock or functionWithBlockParser) }
	}

	fun functionParam() = with(combinator) {
		val selfParser = {
			val token = token(SELF)
			token
				.then { optional(token(COLON).then { expect(type()) }) }
				.map { type -> FunctionParam.SelfParam(+token, type) }
		}

		val patternParser = { patternAndType().map { (pattern, type) -> FunctionParam.PatternParam(pattern, type) } }

		selfParser() or patternParser
	}

	fun struct(): ParseResult<Stmt> = with(combinator) {
		val keyword = token(STRUCT)
		val name = keyword
			.then { expect(token(IDENTIFIER)) }
		val genericParams = name
			.then { optional(genericParams()) }

		val structFieldParser = {
			val name = token(IDENTIFIER)
			name
				.then { expect(token(COLON)) }
				.then { expect(type()) }
				.map { type -> Pair(+name, type) }
		}

		val structParser = {
			token(LEFT_BRACE)
				.then { manyTrailingUntil(structFieldParser, COMMA, RIGHT_BRACE) }
				.map { (list, close) -> Stmt.Struct(+keyword, +name, +genericParams, list, close) }
		}
		val tupleParser = {
			val result = token(LEFT_PAREN)
				.then { manyTrailingUntil(::type, COMMA, RIGHT_PAREN) }
				.map { (list, close) -> Stmt.TupleStruct(+keyword, +name, +genericParams, list, close) }
			result
				.then { expect(token(SEMICOLON)) }
				.flatMap { result }
		}
		val unitParser = { token(SEMICOLON).map { semicolon -> Stmt.UnitStruct(+keyword, +name, +genericParams, semicolon) } }


		genericParams
			.then { expect(structParser() or tupleParser or unitParser) }
	}

	fun module(): ParseResult<Stmt> = with(combinator) {
		val keyword = token(MOD)
		val name = keyword
			.then { expect(token(IDENTIFIER)) }
		name
			.then { expect(token(LEFT_BRACE)) }
			.then { expect(manyUntil(::declaration, RIGHT_BRACE)) }
			.map { (list, close) -> Stmt.Module(+keyword, +name, list, close) }
	}

	fun let(): ParseResult<Stmt> = with(combinator) {
		val keyword = token(LET)
		val patternAndType = keyword
			.then { expect(patternAndOptionalType()) }

		val initializer = patternAndType
			.then { optional(token(EQUAL).then { expect(expression()) }) }

		initializer.then {
			expect(token(SEMICOLON))
				.map { Stmt.Let(+keyword, (+patternAndType).first, (+patternAndType).second, +initializer) }
		}
	}


	// expressions

	fun expressionWithBlock(): ParseResult<Expr> = with(combinator) {
		ifExpr() or ::whileExpr or ::loop or ::forExpr or ::match or ::block
	}

	fun expression(): ParseResult<Expr> = with(combinator) {
		assignment().error { CompilerError.expectedExpression(current) }
	}

	fun ifExpr() = with(combinator) {
		fun ifParser(keyword: Token): ParseResult<Expr> {
			fun elseParser(): ParseResult<Expr> {
				return token(ELSE)
					.then {
						token(IF).flatMap { t -> ifParser(t) } or ::block
					}
			}

			val condition = expect(expression())
			val block = condition
				.then { expect(block()) }
			return block
				.then { optional(elseParser()) }
				.map { elseExpr -> Expr.If(keyword, +condition, +block, elseExpr) }
		}

		token(IF).flatMap { token -> ifParser(token) }
	}

	fun whileExpr() = with(combinator) {
		val keyword = token(WHILE)
		val condition = keyword
			.then { expect(expression()) }
		condition
			.then { expect(block()) }
			.map { block -> Expr.While(+keyword, +condition, block) }
	}

	fun loop() = with(combinator) {
		val keyword = token(LOOP)
		keyword
			.then { expect(block()) }
			.map { block -> Expr.Loop(+keyword, block) }
	}

	fun forExpr() = with(combinator) {
		val keyword = token(FOR)
		val pattern = keyword
			.then { expect(pattern()) }
		val iterator = pattern
			.then { expect(token(IN)) }
			.then { expect(expression()) }
		iterator
			.then { expect(block()) }
			.map { block -> Expr.For(+keyword, +pattern, +iterator, block) }
	}

	fun match() = with(combinator) {
		val branchParser = {
			val pattern = pattern()
			pattern
				.then { expect(token(EQUALS_ARROW)) }
				.then { expect(expression()) }
				.map { expr -> Pair(+pattern, expr) }
		}

		val keyword = token(MATCH)
		val expr = keyword
			.then { expect(expression()) }
		expr
			.then { expect(token(LEFT_BRACE)) }
			.then { expect(manyTrailingUntil(branchParser, COMMA, RIGHT_BRACE)) }
			.map { (branches, close) -> Expr.Match(+keyword, +expr, branches, close) }
	}

	fun primary() = with(combinator) {
		lateinit var open: Token
		val group = {
			token(LEFT_PAREN)
				.with { open = it }
				.then { expect(manySeparatedUntil(::expression, COMMA, RIGHT_PAREN)) }
				.map { (list, close) -> Expr.Group(open, list, close) }
		}
		val literal = {
			token(
				NUMBER,
				STRING,
				TRUE,
				FALSE
			).map { Expr.Literal(it) }
		}
		val variable = { token(IDENTIFIER).map { Expr.Variable(it) } }

		group() or literal or variable or { qualifiedPath().map { path -> Expr.Path(path) } } or ::returnExpr or ::breakExpr or ::closure or ::expressionWithBlock
	}

	fun args(open: TokenType, close: TokenType): ParseResult<Triple<Token, List<Expr>, Token>> {
		return with(combinator) {
			lateinit var openToken: Token
			token(open)
				.with { openToken = it }
				.then { expect(manyTrailingUntil(::expression, COMMA, close)) }
				.map { (list, closeToken) -> Triple(openToken, list, closeToken) }
		}
	}

	fun call(): ParseResult<Expr> = with(combinator) {
		primary().flatMap { expr -> callPrime(expr) }
	}

	fun callPrime(callee: Expr): ParseResult<Expr> = with(combinator) {
		val callArgs = { args(LEFT_PAREN, RIGHT_PAREN).map { (open, args, close) -> Expr.Call(callee, open, args, close) } }
		val indexArgs = { args(LEFT_BRACKET, RIGHT_BRACKET).map { (open, args, close) -> Expr.Index(callee, open, args, close) } }
		val dot = { token(DOT).then { expect(pathSegment()) }.map { seg -> Expr.FieldGet(callee, seg) } }
		val path = {
			pathAfterExpr().map { list ->
				val path = exprLeadingPath(callee, list)
				if (path is ComplexPath.Error) return@map Expr.Path(path)
				Expr.Path(path)
			}
		}

		val args = { callArgs() or indexArgs or dot or path }

		args().flatMap { expr -> callPrime(expr) } or { success(callee) }
	}


	fun unary(): ParseResult<Expr> = with(combinator) {
		val operator = token(MINUS, BANG, TILDA)
		val unary = operator
			.then(::unary)
			.map { expr -> Expr.Unary(+operator, expr) }

		unary or ::call
	}

	fun cast(): ParseResult<Expr> =
		binary(::unary, ::cast, AS)

	fun factor(): ParseResult<Expr> =
		binary(::cast, ::factor, STAR, SLASH, PERCENT)

	fun term(): ParseResult<Expr> =
		binary(::factor, ::term, PLUS, MINUS)

	fun range(): ParseResult<Expr> = with(combinator) {
		val left = optional(term())
		val rightParser = {
			val operator = token(DOT_DOT)
			operator
				.then { optional(range()) }
				.map { expr -> Expr.Range(+left, +operator, expr) }
		}

		left
			.then { optional(rightParser()) }
			.flatMap { it?.let { success(it) } ?: (+left)?.let { success(it) } ?: noMatch(CompilerError.expectedExpression(current)) }
	}

	fun shift(): ParseResult<Expr> =
		binary(::range, ::shift) // TODO: lex << >> >>>

	fun bitAnd(): ParseResult<Expr> =
		binary(::shift, ::bitAnd, AMP)

	fun xor(): ParseResult<Expr> =
		binary(::bitAnd, ::xor, CARET)

	fun bitOr(): ParseResult<Expr> =
		binary(::xor, ::bitOr, PIPE)

	fun comparison(): ParseResult<Expr> =
		binary(::bitOr, ::comparison, EQUAL_EQUAL, BANG_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)

	fun and(): ParseResult<Expr> =
		binary(::comparison, ::and, AMP_AMP)

	fun or(): ParseResult<Expr> =
		binary(::and, ::or, PIPE_PIPE)

	fun assignment(): ParseResult<Expr> = // TODO: lex <<= >>= >>>=
		binary(::or, ::assignment, EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, PERCENT_EQUAL, PIPE_EQUAL, AMP_EQUAL, CARET_EQUAL)


	inline fun binary(left: () -> ParseResult<Expr>, crossinline right: () -> ParseResult<Expr>, vararg symbols: TokenType) = with(combinator) {
		val left = left()

		val rightParser = {
			val operator = token(*symbols)
			operator
				.then { expect(right()) }
				.map { expr -> Expr.Binary(+left, +operator, expr) }
		}

		left
			.then { optional(rightParser()) }
			.map { it ?: +left }
	}

	fun returnExpr() = with(combinator) {
		val keyword = token(RETURN)
		keyword
			.then { optional(expression()) }
			.map { expr -> Expr.Return(+keyword, expr) }
	}

	fun breakExpr() = with(combinator) {
		val keyword = token(BREAK)
		keyword
			.then { optional(expression()) }
			.map { expr -> Expr.Break(+keyword, expr) }
	}

	fun closure(): ParseResult<Expr.Closure> = with(combinator) {
		val beginNoArgs = {
			val doublePipe = token(PIPE_PIPE)
			doublePipe.map { token -> Triple(token, emptyList<Pair<Pattern, Type?>>(), token) }
		}
		val beginArgs = {
			val open = token(PIPE)
			open
				.then { expect(manyTrailingUntil(::patternAndOptionalType, COMMA, PIPE)) }
				.map { (list, close) -> Triple(+open, list, close) }
		}
		val params = (beginArgs() or beginNoArgs)
		params
			.then { expect(expression()) }
			.map { expr -> (+params).let { (open, list, close) -> Expr.Closure(open, list, close, expr) } }
	}

	fun block(): ParseResult<Expr.Block> = with(combinator) {
		fun trailingCheck(expr: Expr) =
			when (expr) {
				is Expr.Block, is Expr.Loop, is Expr.If -> Stmt.Expression(expr, null)
				else -> {
					val semicolon = token(SEMICOLON)
					if (semicolon.isSuccess()) Stmt.Expression(expr, +semicolon)
					else {
						val expr = Stmt.Expression(expr, null)
						if (!isNext(RIGHT_BRACE)) {
							expect(token(SEMICOLON)) // create error message
							null
						} else expr
					}
				}
			}


		val open = token(LEFT_BRACE)
		open
			.then {
				expect(
					manyUntil(
						{ declaration() or ::let or { expression().flatMap { trailingCheck(it)?.let { success(it) } ?: failure() } } },
						RIGHT_BRACE
					)
				)
			}
			.map { (list, close) -> Expr.Block(+open, list, close) }

	}


	// patterns
	fun pattern(): ParseResult<Pattern> = with(combinator) {
		(pathPattern() or ::wildCard or ::literalPattern or ::tuplePattern)
			.error { CompilerError.expectedPattern(current) }
	}

	fun pathPattern() = with(combinator) { token(IDENTIFIER).map { t -> Pattern.Identifier(t) } }
	fun wildCard() = with(combinator) { token(UNDERSCORE).map { t -> Pattern.Wildcard(t) } }
	fun literalPattern() = with(combinator) { token(NUMBER, STRING, TRUE, FALSE).map { t -> Pattern.Literal(t) } }
	fun tuplePattern() = with(combinator) {
		val open = token(LEFT_PAREN)
		open
			.then { expect(manyTrailingUntil(::pattern, COMMA, RIGHT_PAREN)) }
			.map { (list, close) -> Pattern.Tuple(+open, list, close) }
	}

	fun patternAndType(): ParseResult<Pair<Pattern, Type>> = with(combinator) {
		val pattern = pattern()
		pattern
			.then { expect(token(COLON)) }
			.then { expect(type()) }
			.map { type -> Pair(+pattern, type) }
	}

	fun patternAndOptionalType(): ParseResult<Pair<Pattern, Type?>> = with(combinator) {
		val pattern = pattern()
		pattern
			.then { optional(token(COLON).then { expect(type()) }) }
			.map { type -> Pair(+pattern, type) }
	}

	// types
	fun type(): ParseResult<Type> = with(combinator) {
		val tupleParser = {
			val open = token(LEFT_PAREN)
			open
				.then { expect(manyTrailingUntil({ expect(type()) }, COMMA, RIGHT_PAREN)) }
				.map { (list, close) -> Type.Tuple(+open, list, close) }
		}

		(tupleParser() or { complexPath().map { path -> Type.Path(path) } })
			.error { CompilerError.expectedType(current) }
	}

	fun genericArgs(): ParseResult<GenericArgs> = with(combinator) {
		val open = token(LEFT_BRACKET)
		open
			.then { expect(manyTrailingUntil({ expect(type()) }, COMMA, RIGHT_BRACKET)) }
			.map { (list, close) -> GenericArgs(+open, list, close) }
	}

	fun genericParams(): ParseResult<GenericParams> = with(combinator) {
		val open = token(LEFT_BRACKET)
		val typeParamParser = {
			val identifier = token(IDENTIFIER)
			identifier
				.then { optional(token(COLON).then { expect(typeBounds()) }) }
				.map { bounds -> Pair(+identifier, bounds) }
		}
		open
			.then { expect(manyTrailingUntil(typeParamParser, COMMA, RIGHT_BRACKET)) }
			.map { (list, close) -> GenericParams(+open, list, close) }
	}

	fun typeBounds(): ParseResult<TypeParamBounds> = with(combinator) {
		val first = normalComplexPath()
		first
			.then { many({ token(AMP).then { expect(normalComplexPath()) } }, listOf(+first)) }
			.map { list -> TypeParamBounds(list) }
	}

	// paths

	fun normalComplexPath() = with(combinator) {
		val firstSegment = pathSegment()
		firstSegment
			.then { expect(many({ token(COLON_COLON).then { expect(pathSegment()) } }, listOf(+firstSegment))) }
			.map { segments -> ComplexPath.Normal(segments) }
	}

	fun qualifiedPath() = with(combinator) {
		val asParser = { token(AS).then { expect(normalComplexPath()) } }

		val openToken = token(LESS)
		val type = openToken
			.then { expect(type()) }
		val trait = type
			.then { optional(asParser()) }
		val close = trait
			.then { expect(token(GREATER)) }
		val firstSegment = close
			.then { expect(token(COLON_COLON).error { CompilerError.expectedPathSegment(current) }.then(::pathSegment)) }
		firstSegment
			.then { many({ token(COLON_COLON).then { expect(pathSegment()) } }, listOf(+firstSegment)) }
			.map { segments -> ComplexPath.Qualified(+openToken, +type, +trait, +close, segments) }

	}

	fun complexPath(): ParseResult<ComplexPath> = with(combinator) {
		qualifiedPath() or ::normalComplexPath
	}


	fun pathAfterExpr(): ParseResult<List<ComplexSegment>> = with(combinator) {
		val first = token(COLON_COLON)
			.then { expect(pathSegment()) }
		first
			.then { many({ token(COLON_COLON).then { expect(pathSegment()) } }, listOf(+first)) }
	}

	fun pathSegment(): ParseResult<ComplexSegment> = with(combinator) {
		val identifier = token(IDENTIFIER)
		identifier
			.then { optional(genericArgs()) }
			.map { generics -> ComplexSegment(+identifier, generics) }
	}

	// utility

	private fun exprLeadingPath(expr: Expr, segments: List<ComplexSegment>): ComplexPath {
		return when (expr) {
			is Expr.Index ->
				if (expr.callee is Expr.Variable)
					ComplexPath.Normal(buildList {
						add(ComplexSegment(expr.callee.identifier, GenericArgs(expr.argOpen, expr.args.map { exprToType(it) }, expr.argClose)))
						addAll(segments)
					})
				else {
					zinc.reportCompileError(CompilerError.expectedPathSegment(expr))
					ComplexPath.Error(expr.range().start..segments.last().let { it.generics?.end ?: it.id })
				}

			is Expr.Variable ->
				ComplexPath.Normal(buildList {
					add(ComplexSegment(expr.identifier, null))
					addAll(segments)
				})

			else -> {
				zinc.reportCompileError(CompilerError.expectedPathSegment(expr))
				ComplexPath.Error(expr.range().start..segments.last().let { it.generics?.end ?: it.id })
			}
		}
	}

	private fun exprToType(expr: Expr): Type {
		return when (expr) {
			is Expr.Variable -> Type.Path(ComplexPath.Normal(listOf(ComplexSegment(expr.identifier, null))))

			is Expr.Index -> if (expr.callee !is Expr.Variable) {
				zinc.reportCompileError(CompilerError.expectedType(expr))
				Type.Error
			} else Type.Path(
				ComplexPath.Normal(
					listOf(ComplexSegment(expr.callee.identifier, GenericArgs(expr.argOpen, expr.args.map { exprToType(it) }, expr.argClose)))
				)
			)

			is Expr.Path -> Type.Path(expr.path)
			is Expr.Group -> Type.Tuple(expr.open, expr.expressions.map { exprToType(it) }, expr.close)
			else -> {
				zinc.reportCompileError(CompilerError.expectedType(expr))
				Type.Error
			}
		}
	}

}