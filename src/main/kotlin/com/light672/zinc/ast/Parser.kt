package com.light672.zinc.ast

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.TokenType.*

internal class Parser(val zinc: Zinc.Runtime) {
	val combinator = CombinatorParser(zinc)

	fun declaration(): ParseResult<Stmt> = with(combinator) {
		(function() or ::struct or ::module or ::trait or ::implementation).error { CompilerError.expectedStatement(current) }
	}

	fun associatedStmt(): ParseResult<AssociatedStmt> = with(combinator) {
		(function()).error { CompilerError.expectedAssociatedStatement(current) }
	}

	fun trait(): ParseResult<Stmt.Trait> = with(combinator) {
		val keyword = token(INTERFACE)
		val name = keyword
			.then { expect(token(IDENTIFIER)) }
		val genericParams = name
			.then { optional(genericParams()) }
		val whereClause = genericParams
			.then { optional(where()) }
		whereClause
			.then { expect(token(LEFT_BRACE)) }
			.then { manyUntil(::associatedStmt, RIGHT_BRACE) }
			.map { (stmts, close) -> Stmt.Trait(+keyword, +name, +genericParams, +whereClause, stmts) }
	}

	fun implementation(): ParseResult<Stmt.Implementation> = with(combinator) {
		val keyword = token(IMPL)
		val genericParams = keyword
			.then { optional(genericParams()) }
		val type = genericParams
			.then { expect(type()) }
		val trait = type
			.then { optional(token(COLON).then { expect(typePath()) }) }
		val whereClause = trait
			.then { optional(where()) }

		whereClause
			.then { expect(token(LEFT_BRACE)) }
			.then { manyUntil(::associatedStmt, RIGHT_BRACE) }
			.map { (stmts, close) -> Stmt.Implementation(+keyword, +genericParams, +type, +trait, +whereClause, stmts) }
	}

	fun function(): ParseResult<Stmt.Function> = with(combinator) {
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
					.map { (list, close) -> Triple(+open, list, close) }
			})
		}
		val returnType = params.then { optional(token(MINUS_ARROW).then { expect(type()) }) }
		val whereClause = returnType.then { optional(where()) }
		val functionNoBlock =
			token(SEMICOLON).map { _ ->
				Stmt.Function(+keyword, +name, +genericParams, (+params).second, (+params).third, +returnType, +whereClause, null)
			}

		val functionWithBlockParser =
			{
				block(null).map { block ->
					Stmt.Function(+keyword, +name, +genericParams, (+params).second, (+params).third, +returnType, +whereClause, block)
				}
			}
		whereClause
			.then { expect(functionNoBlock or functionWithBlockParser) }
	}

	fun functionParam() = with(combinator) {
		val selfParser = {
			val token = token(SELF)
			token
				.then { optional(token(COLON).then { expect(type()) }) }
				.map { type -> FunctionParam.Self(+token, type) }
		}

		val patternParser = { patternAndType().map { (pattern, type) -> FunctionParam.Pattern(pattern, type) } }

		selfParser() or patternParser
	}

	fun struct(): ParseResult<Stmt> = with(combinator) {
		val keyword = token(STRUCT)
		val name = keyword
			.then { expect(token(IDENTIFIER)) }
		val genericParams = name
			.then { optional(genericParams()) }

		val whereClause = genericParams.then { optional(where()) }

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
				.map { (list, close) -> Stmt.Struct(+keyword, +name, +genericParams, list, +whereClause, close) }
		}
		val tupleParser = {
			val result = token(LEFT_PAREN)
				.then { manyTrailingUntil(::type, COMMA, RIGHT_PAREN) }
				.map { (list, close) -> Stmt.TupleStruct(+keyword, +name, +genericParams, list, +whereClause, close) }
			result
				.then { expect(token(SEMICOLON)) }
				.flatMap { result }
		}
		val unitParser = { token(SEMICOLON).map { semicolon -> Stmt.UnitStruct(+keyword, +name, +genericParams, +whereClause, semicolon) } }

		whereClause
			.then { expect(structParser() or tupleParser or unitParser) }
	}

	fun module(): ParseResult<Stmt.Module> = with(combinator) {
		val keyword = token(MOD)
		val name = keyword
			.then { expect(token(IDENTIFIER)) }
		name
			.then { expect(token(LEFT_BRACE)) }
			.then { expect(manyUntil(::declaration, RIGHT_BRACE)) }
			.map { (list, close) -> Stmt.Module(+keyword, +name, list, close) }
	}

	fun let(): ParseResult<Stmt.Let> = with(combinator) {
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

	fun exprWithLabel(): ParseResult<Expr> = with(combinator) {
		val label = token(IDENTIFIER)
		if (label.isSuccess() && !isNext(COLON)) return label.flatMap { begin -> exprPath(begin) }


		val parser = { label: Token? -> ifExpr(label) or { whileExpr(label) } or { loop(label) } or { forExpr(label) } or { block(label) } }

		label
			.then {
				expect(token(COLON))
					.then { expect(parser(+label).error { CompilerError.expectedBlockExpr(current) }) }
			}
			.or { parser(null) }
	}

	fun expression(): ParseResult<Expr> = with(combinator) {
		assignment().error { CompilerError.expectedExpression(current) }
	}

	fun ifExpr(label: Token?) = with(combinator) {
		fun ifParser(keyword: Token): ParseResult<Expr> {
			fun elseParser(): ParseResult<Expr> {
				return token(ELSE)
					.then {
						token(IF).flatMap { t -> ifParser(t) } or { block(null) }
					}
			}

			val condition = expect(expression())
			val block = condition
				.then { expect(block(null)) }
			return block
				.then { optional(elseParser()) }
				.map { elseExpr -> Expr.If(label, keyword, +condition, +block, elseExpr) }
		}

		token(IF).flatMap { token -> ifParser(token) }
	}

	fun whileExpr(label: Token?) = with(combinator) {
		val keyword = token(WHILE)
		val condition = keyword
			.then { expect(expression()) }
		condition
			.then { expect(block(null)) }
			.map { block -> Expr.While(label, +keyword, +condition, block) }
	}

	fun loop(label: Token?) = with(combinator) {
		val keyword = token(LOOP)
		keyword
			.then { expect(block(null)) }
			.map { block -> Expr.Loop(label, +keyword, block) }
	}

	fun forExpr(label: Token?) = with(combinator) {
		val keyword = token(FOR)
		val pattern = keyword
			.then { expect(pattern()) }
		val iterator = pattern
			.then { expect(token(IN)) }
			.then { expect(expression()) }
		iterator
			.then { expect(block(null)) }
			.map { block -> Expr.For(label, +keyword, +pattern, +iterator, block) }
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
		val group = {
			val open = token(LEFT_PAREN)
			open
				.then { expect(manySeparatedUntil(::expression, COMMA, RIGHT_PAREN)) }
				.map { (list, close) -> Expr.Group(+open, list, close) }
		}
		val literal = {
			token(
				NUMBER,
				STRING,
				TRUE,
				FALSE
			).map { Expr.Literal(it) }
		}

		group() or
				literal or
				{ qualifiedPath().map { path -> Expr.Path(path) } } or
				::returnExpr or
				::breakExpr or
				::continueExpr or
				::closure or
				::match or
				::exprWithLabel
	}

	fun args(open: TokenType, close: TokenType): ParseResult<Triple<Token, List<Expr>, Token>> {
		return with(combinator) {
			val openToken = token(open)
			openToken
				.then { expect(manyTrailingUntil(::expression, COMMA, close)) }
				.map { (list, closeToken) -> Triple(+openToken, list, closeToken) }
		}
	}

	fun call(): ParseResult<Expr> = with(combinator) {
		primary().flatMap { expr -> callPrime(expr) }
	}

	fun callPrime(callee: Expr): ParseResult<Expr> = with(combinator) {
		val callArgs = { args(LEFT_PAREN, RIGHT_PAREN).map { (open, args, close) -> Expr.Call(callee, open, args, close) } }
		val indexArgs = { args(LEFT_BRACKET, RIGHT_BRACKET).map { (open, args, close) -> Expr.Index(callee, open, args, close) } }
		val dot = { token(DOT).then { expect(fieldSegment()) }.map { seg -> Expr.FieldGet(callee, seg) } }

		val args = { callArgs() or indexArgs or dot }

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
		val label = keyword
			.then { optional(token(AT).then { expect(token(IDENTIFIER)) }) }
		label
			.then { optional(expression()) }
			.map { expr -> Expr.Break(+keyword, +label, expr) }
	}

	fun continueExpr() = with(combinator) {
		val keyword = token(CONTINUE)
		keyword
			.then { optional(token(AT).then { expect(token(IDENTIFIER)) }) }
			.map { label -> Expr.Continue(+keyword, label) }
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

		val returnTypeParser = {
			val type = token(MINUS_ARROW).then { expect(type()) }
			type.then { expect(block(null)) }
				.map { block -> Pair(+type, block) }
		}

		val withoutReturnType = {
			expression()
				.map { expr -> Pair(null, expr) }
		}

		val returnTypeAndExpr = params
			.then { returnTypeParser() or withoutReturnType }
		returnTypeAndExpr
			.map { (type, expr) -> (+params).let { (open, list, close) -> Expr.Closure(open, list, close, type, expr) } }
	}

	fun block(label: Token?): ParseResult<Expr.Block> = with(combinator) {
		fun trailingCheck(expr: Expr) =
			when (expr) {
				is Expr.Block,
				is Expr.Loop,
				is Expr.If -> Stmt.Expression(expr, null)

				else       -> {
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
			.map { (list, close) -> Expr.Block(label, +open, list, close) }

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
		val open = token(LESS)
		open
			.then { expect(manyTrailingUntil({ expect(type()) }, COMMA, GREATER)) }
			.map { (list, close) -> GenericArgs(+open, list, close) }
	}

	fun genericParams(): ParseResult<GenericParams> = with(combinator) {
		val open = token(LESS)
		val typeParamParser = {
			val identifier = token(IDENTIFIER)
			identifier
				.then { optional(token(COLON).then { expect(typeParamBound()) }) }
				.map { bounds -> GenericParam(+identifier, bounds) }
		}
		open
			.then { expect(manyTrailingUntil(typeParamParser, COMMA, GREATER)) }
			.map { (list, close) -> GenericParams(+open, list, close) }
	}

	fun where(): ParseResult<WhereClause> = with(combinator) {
		val keyword = token(WHERE)
		val clauseParser = {
			val type = type()
			val bounds = type
				.then { expect(token(COLON)).then { expect(typeParamBound()) } }
			bounds.map { bounds -> WhereClauseItem(+type, bounds) }
		}
		val first = clauseParser()
		first
			.then { manyTrailing(clauseParser, COMMA) }
			.map { clauseItems -> WhereClause(+keyword, clauseItems) }
	}

	fun typeParamBound(): ParseResult<TypeParamBounds> = with(combinator) {
		val first = typePath()
		first
			.then { many({ token(AMP).then { expect(typePath()) } }, listOf(+first)) }
			.map { list -> TypeParamBounds(list) }
	}

	// paths

	fun path(segmentParser: () -> ParseResult<ComplexSegment>) = path(segmentParser, segmentParser)

	fun path(beginParser: () -> ParseResult<ComplexSegment>, restParser: () -> ParseResult<ComplexSegment>) = with(combinator) {
		val list = ArrayList<ComplexSegment>()

		var topSegment = beginParser()
		if (!topSegment.isSuccess()) return ParseResult.NoMatch(CompilerError.EMPTY)
		list.add(+topSegment)

		while (isPrevious(COLON_COLON))
			topSegment = topSegment
				.then { expect(restParser()) }
				.with { seg -> list.add(seg) }

		topSegment
			.flatMap { success(ComplexPath.Normal(list)) }
	}

	fun typePath() = path(::pathSegment)
	fun exprPath(beginToken: Token) = with(combinator) {
		path({ exprPathSegment(beginToken) }, ::exprPathSegment)
			.map { path -> Expr.Path(path) }
	}


	fun qualifiedPath() = with(combinator) {
		val asParser = { token(AS).then { expect(typePath()) } }

		val openToken = token(LESS)
		val type = openToken
			.then { expect(type()) }
		val trait = type
			.then { optional(asParser()) }
		val close = trait
			.then { expect(token(GREATER)) }
		close
			.then { expect(token(COLON_COLON)) }
			.then { expect(typePath()) }
			.map { typePath -> ComplexPath.Qualified(+openToken, +type, +trait, +close, typePath.body) }

	}

	fun complexPath(): ParseResult<ComplexPath> = with(combinator) {
		qualifiedPath() or ::typePath
	}

	fun pathSegment(): ParseResult<ComplexSegment> = with(combinator) {
		val identifier = token(IDENTIFIER)
		identifier
			.then { optional(token(COLON_COLON)) } // consumes the `::` in between ident and generics if people use that for some reason
			.then { optional(genericArgs().with { token(COLON_COLON) }) } // consumes terminator `::` after generic args if present
			.map { generics -> ComplexSegment(+identifier, generics) }
	}


	// only difference between this and `pathSegment` is that a `::` must be present before parsing any generics
	fun exprPathSegment(identifier: Token? = null): ParseResult<ComplexSegment> = with(combinator) {
		val identifier = identifier?.let { success(it) } ?: token(IDENTIFIER)
		identifier
			.then { optional(token(COLON_COLON).then { optional(genericArgs().with { token(COLON_COLON) }) }) } // consumes terminator `::` after generic args if present
			.map { generics -> ComplexSegment(+identifier, generics) }
	}

	fun fieldSegment(): ParseResult<ComplexSegment> = with(combinator) {
		val identifier = token(IDENTIFIER)
		identifier
			.then { optional(token(COLON_COLON).then { expect(genericArgs()) }) }
			.map { generics -> ComplexSegment(+identifier, generics) }
	}

}