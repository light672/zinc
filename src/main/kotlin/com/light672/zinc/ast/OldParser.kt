package com.light672.zinc.ast
/*
import com.light672.zinc.CompilerError
import com.light672.zinc.Either
import com.light672.zinc.Scope
import com.light672.zinc.Zinc
import com.light672.zinc.ast.TokenType.*
import com.light672.zinc.ir.Item


internal class Parser(private val zinc: Zinc.Runtime) {
	private val lexer = Lexer(zinc.source, zinc)
	private var previous = Token.empty()
	private var current = lexer.scanToken()

	private lateinit var scope: Scope

	fun parse(scope: Scope): List<Stmt>? {
		this.scope = scope
		val statements = ArrayList<Stmt>()
		while (!atEnd()) {
			val declaration = declaration() ?: return null
			statements.add(declaration)
			addToScope(scope, declaration)
		}

		return statements
	}

	// declarations

	private fun declaration(): Stmt? {
		return when (current.type) {
			FN -> funcDeclaration(consume())
			STRUCT -> structDeclaration(consume())
			ENUM -> enumDeclaration(consume())
			INTERFACE -> interfaceDeclaration(consume())
			IMPL -> implDeclaration(consume())
			TYPEALIAS -> typeAliasDeclaration(consume())
			CONST -> constDeclaration(consume())
			else -> TODO("throw error")
		}
	}

	private fun associatedDeclaration(): Stmt.AssociatedAllowed? {
		return when (current.type) {
			FN -> funcDeclaration(consume()) ?: return null
			TYPEALIAS -> typeAliasDeclaration(consume()) ?: return null
			CONST -> constDeclaration(consume()) ?: return null
			else -> TODO("throw error")
		}
	}

	private fun funcDeclaration(keyword: Token): Stmt.Function? {
		val name = expect(IDENTIFIER) ?: return null
		val genericParams =
			if (match(LESS)) genericParams(previous) ?: return null
			else null
		val start = expect(LEFT_PAREN) ?: return null
		val (params, end) = trailingCommaGroup(RIGHT_PAREN) {
			val pattern = expectPattern() ?: return null
			expect(COLON) ?: return null
			val type = expectType() ?: return null
			Pair(pattern, type)
		} ?: return null
		val returnType = if (match(MINUS_ARROW)) expectType() ?: return null else null
		val blockOrSemi = if (match(SEMICOLON)) Either.Right(previous) else {
			expect(LEFT_BRACE) ?: return null
			Either.Left(block() ?: return null)
		}
		return Stmt.Function(keyword, name, genericParams, params, returnType, blockOrSemi)
	}

	private fun structDeclaration(keyword: Token): Stmt? {
		val name = expect(IDENTIFIER) ?: return null
		val genericParams =
			if (match(LESS)) genericParams(previous) ?: return null
			else null
		if (match(SEMICOLON))
			return Stmt.UnitStruct(keyword, name, genericParams, previous)

		if (match(LEFT_PAREN))
			return trailingCommaGroup(RIGHT_PAREN, ::expectType)?.let { (fields, _) -> Stmt.TupleStruct(keyword, name, genericParams, fields) }

		expect(LEFT_BRACE) ?: return null
		val (fields, _) = trailingCommaGroup(RIGHT_BRACE) {
			val name = expect(IDENTIFIER) ?: return null
			expect(COLON) ?: return null
			val type = expectType() ?: return null
			Pair(name, type)
		} ?: return null
		return Stmt.Struct(keyword, name, genericParams, fields)
	}

	private fun enumDeclaration(keyword: Token): Stmt? {
		val name = expect(IDENTIFIER) ?: return null
		val genericParams =
			if (match(LESS)) genericParams(previous) ?: return null
			else null
		expect(LEFT_BRACE) ?: return null
		val (variants, close) = trailingCommaGroup(RIGHT_BRACE, ::enumVariant) ?: return null
		return Stmt.Enum(keyword, name, genericParams, variants)
	}

	private fun enumVariant(): Stmt.Enum.Variant? {
		val name = expect(IDENTIFIER) ?: return null
		if (match(LEFT_PAREN))
			return trailingCommaGroup(RIGHT_PAREN, ::expectType)?.let { (fields, _) -> Stmt.Enum.Variant.Tuple(name, fields) }
		if (match(LEFT_BRACE))
			return trailingCommaGroup(RIGHT_BRACE) {
				val name = expect(IDENTIFIER) ?: return null
				expect(COLON) ?: return null
				val type = expectType() ?: return null
				Pair(name, type)
			}?.let { (fields, _) -> Stmt.Enum.Variant.Struct(name, fields) }
		return Stmt.Enum.Variant.Unit(name)
	}

	private fun interfaceDeclaration(keyword: Token): Stmt.Trait? {
		val name = expect(IDENTIFIER) ?: return null
		val genericParams =
			if (match(LESS)) genericParams(previous) ?: return null
			else null
		expect(LEFT_BRACE) ?: return null
		val declarations = ArrayList<Stmt.AssociatedAllowed>()
		while (!isNext(RIGHT_BRACE))
			declarations.add(associatedDeclaration() ?: return null)
		expect(RIGHT_BRACE) ?: return null
		return Stmt.Trait(keyword, name, genericParams, declarations)
	}

	private fun implDeclaration(keyword: Token): Stmt.Implementation? {
		val genericParams =
			if (match(LESS)) genericParams(previous) ?: return null
			else null

		val type = expectType() ?: return null
		val inheriting =
			if (match(COLON)) expectType() ?: return null
			else null

		expect(LEFT_BRACE) ?: return null
		val declarations = ArrayList<Stmt.AssociatedAllowed>()
		while (!isNext(RIGHT_BRACE))
			declarations.add(associatedDeclaration() ?: return null)
		expect(RIGHT_BRACE) ?: return null
		return Stmt.Implementation(keyword, genericParams, type, inheriting, declarations)
	}

	private fun typeAliasDeclaration(keyword: Token): Stmt.TypeAlias? {
		val name = expect(IDENTIFIER) ?: return null
		val genericParams =
			if (match(LESS)) genericParams(previous) ?: return null
			else null
		val initializer =
			if (match(EQUAL)) expectType() ?: return null
			else null

		return Stmt.TypeAlias(keyword, name, genericParams, initializer)
	}

	private fun constDeclaration(keyword: Token): Stmt.Const? {
		val name = expect(IDENTIFIER) ?: return null
		expect(EQUAL) ?: return null
		val initializer = expression() ?: return null

		return Stmt.Const(keyword, name, initializer)
	}

	// statements
	private fun statement(): Stmt? {
		return when (current.type) {
			FN -> funcDeclaration(consume())
			STRUCT -> structDeclaration(consume())
			ENUM -> enumDeclaration(consume())
			INTERFACE -> interfaceDeclaration(consume())
			IMPL -> implDeclaration(consume())
			TYPEALIAS -> typeAliasDeclaration(consume())
			CONST -> constDeclaration(consume())
			LET -> letStatement(consume())
			else -> exprStatement()
		}
	}

	private fun letStatement(keyword: Token): Stmt.Let? {
		val pattern = expectPattern() ?: return null
		val type =
			if (match(COLON)) expectType() ?: return null
			else null
		val initializer =
			if (match(EQUAL)) expression() ?: return null
			else null
		return Stmt.Let(keyword, pattern, type, initializer)
	}

	private fun exprStatement(): Stmt.Expression? {
		val expression = expression() ?: return null
		val trailing = when (expression) {
			is Expr.Block -> if (match(SEMICOLON)) previous else null
			else -> if (isNext(RIGHT_BRACE)) null else {
				expect(SEMICOLON) ?: return null
				previous
			}
		}
		return Stmt.Expression(expression, trailing)
	}

	// expressions
	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT)

	private fun parsePrecedence(
		precedence: Precedence,
	): Expr? {
		val prefixToken = consume()
		val rule = prefix(prefixToken) ?: return null
		var left = rule() ?: return null
		while (precedence.ordinal <= ((current as? Token)?.type?.precedence ?: Precedence.NONE).ordinal) {
			val infixOperator = consume()
			val infix = infix(infixOperator)
			left = infix(left) ?: return null
		}
		return left
	}

	private fun prefix(token: Token): (() -> Expr?)? {
		return when (token.type) {
			LEFT_PAREN -> ::group
			LEFT_BRACE -> ::block
			MINUS, BANG, TILDA, PLUS_PLUS, MINUS_MINUS -> ::unary
			LESS -> ::qualifiedExpr
			IDENTIFIER -> ::pathExpr
			NUMBER, STRING -> ::literal
			else -> TODO("expected expression")
		}
	}

	private fun infix(token: Token): (Expr) -> Expr? {
		return when (token.type) {
			EQUAL -> ::assignment
			PIPE_PIPE -> ::or
			AMP_AMP -> ::and
			EQUAL_EQUAL, BANG_EQUAL -> ::equality
			LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> ::comparison
			PIPE -> ::bitOr
			CARET -> ::bitXor
			AMP -> ::bitAnd
			PLUS, MINUS -> ::term
			STAR, SLASH -> ::factor
			DOT -> ::get
			LEFT_PAREN -> ::call
			else -> throw IllegalArgumentException("Expected infix function")
		}
	}

	private fun block(): Expr.Block? {
		val start = previous
		val previousScope = scope
		val innerScope = Scope(previousScope)
		scope = innerScope
		val stmts = ArrayList<Stmt>()
		while (!isNext(RIGHT_BRACE)) {
			val statement = statement() ?: return null
			stmts.add(statement)
			addToScope(scope, statement)
		}
		expect(RIGHT_BRACE) ?: return null
		scope = previousScope
		return Expr.Block(start, stmts, previous, innerScope)
	}

	private fun group(): Expr.Group? {
		val start = previous
		val (fields, close) = group(RIGHT_PAREN, ::expression) ?: return null
		return Expr.Group(start, fields, close)
	}

	private fun unary(): Expr.Unary? {
		val op = previous
		val expression = parsePrecedence(Precedence.UNARY) ?: return null
		return Expr.Unary(op, expression)
	}

	private fun pathExpr(): Expr.Path? {
		return complexPath(previous, true)?.let { path -> Expr.Path(Either.Left(path)) }
	}

	private fun qualifiedExpr(): Expr.Path? {
		return qualifiedPath(previous, true)?.let { path -> Expr.Path(Either.Right(path)) }
	}

	private fun literal() = Expr.Literal(previous)

	private fun binary(left: Expr, op: Token, precedence: Precedence): Expr.Binary? {
		val right = parsePrecedence(precedence) ?: return null
		return Expr.Binary(left, op, right)
	}

	private fun assignment(left: Expr) =
		binary(left, previous, Precedence.ASSIGNMENT)

	// infix: '||'
	private fun or(left: Expr) =
		binary(left, previous, Precedence.AND)

	// infix: '&&'
	private fun and(left: Expr) =
		binary(left, previous, Precedence.EQUALITY)

	// infix: '==', '!='
	private fun equality(left: Expr) =
		binary(left, previous, Precedence.COMPARISON)

	// infix: '<', '>', '<=', '>='
	private fun comparison(left: Expr) =
		binary(left, previous, Precedence.BIT_OR)

	// infix: '|'
	private fun bitOr(left: Expr) =
		binary(left, previous, Precedence.BIT_XOR)

	// infix: '^'
	private fun bitXor(left: Expr) =
		binary(left, previous, Precedence.BIT_AND)

	// infix: '&'
	private fun bitAnd(left: Expr) =
		binary(left, previous, Precedence.TERM)

	// infix: '+', '-'
	private fun term(left: Expr) =
		binary(left, previous, Precedence.FACTOR)

	// infix: '*', '/'
	private fun factor(left: Expr) =
		binary(left, previous, Precedence.UNARY)

	// infix: '.'
	private fun get(left: Expr): Expr.Get? {
		expect(IDENTIFIER) ?: return null
		val segment = segmentInExpr(previous) ?: return null
		return Expr.Get(left, segment)
	}

	// infix '('
	private fun call(left: Expr): Expr.Call? {
		val (args, close) = trailingCommaGroup(RIGHT_PAREN, ::expression) ?: return null
		return Expr.Call(left, args, close)
	}

	// patterns
	private fun expectPattern(): Pattern? {
		return when (current.type) {
			LEFT_PAREN -> consume().let { start ->
				trailingCommaGroup(RIGHT_PAREN, ::expectPattern)?.let { (list, end) -> Pattern.Tuple(start, list, end) }
			}

			IDENTIFIER -> {
				consume()
				val path = pathExpr() ?: return null
				if (match(LEFT_BRACE)) trailingCommaGroup(RIGHT_BRACE) {
					val name = expect(IDENTIFIER) ?: return null
					if (!match(COLON)) return@trailingCommaGroup Either.Right(name)
					val pattern = expectPattern() ?: return null
					Either.Left(Pair(name, pattern))
				}?.let { (list, end) -> Pattern.Struct(path, list, end) }
				else if (match(LEFT_PAREN))
					trailingCommaGroup(RIGHT_PAREN, ::expectPattern)?.let { (list, end) -> Pattern.TupleStruct(path, list, end) }
				else
					Pattern.Path(path)
			}

			MUT -> {
				val mut = consume()
				val identifier = expect(IDENTIFIER) ?: return null
				Pattern.Mut(mut, identifier)
			}

			else -> TODO("expected pattern error")
		}
	}

	// generics
	private fun genericParams(start: Token): GenericParams? = trailingCommaGroup(GREATER) {
		val id = expect(IDENTIFIER) ?: return null
		val bounds =
			if (match(COLON_COLON)) expectTypeParamBounds() ?: return null
			else null
		Pair(id, bounds)
	}?.let { (fields, end) -> GenericParams(start, fields, end) }

	private fun genericArgs(start: Token): GenericArgs? = group(GREATER, ::expectType)?.let { (fields, end) -> GenericArgs(start, fields, end) }

	private fun expectTypeParamBounds(): TypeParamBounds? {
		val bounds = arrayListOf(expectType() ?: return null)
		while (match(PLUS)) bounds.add(expectType() ?: return null)
		return TypeParamBounds(bounds)
	}

	// types
	private fun expectType(): Type? {
		return when (current.type) {
			LEFT_PAREN -> consume().let { start ->
				trailingCommaGroup(RIGHT_PAREN, ::expectType)?.let { (fields, end) -> Type.Tuple(start, fields, end) }
			}

			LESS -> qualifiedPath(consume(), false)?.let { path -> Type.Path(Either.Right(path)) }
			IDENTIFIER -> complexPath(consume(), false)?.let { path -> Type.Path(Either.Left(path)) }

			else -> TODO("expected type")
		}
	}

	// paths
	private fun segmentInType(beginning: Token): ComplexSegment? {
		match(COLON_COLON)
		if (!match(LESS)) return ComplexSegment(beginning, null)
		val segment = ComplexSegment(beginning, genericArgs(previous) ?: return null)
		match(COLON_COLON)
		return segment
	}

	private fun segmentInExpr(beginning: Token): ComplexSegment? {
		if (!match(COLON_COLON) || !match(LESS)) return ComplexSegment(beginning, null)
		val segment = ComplexSegment(beginning, genericArgs(previous) ?: return null)
		match(COLON_COLON)
		return segment
	}

	private fun qualifiedSegment(start: Token): QualifiedSegment? {
		val type = expectType() ?: return null
		val asType =
			if (match(AS)) {
				expect(IDENTIFIER) ?: return null
				complexPath(previous, false)
			} else null
		expect(GREATER) ?: return null
		return QualifiedSegment(start, type, asType, previous)
	}

	private fun complexPath(beginning: Token, inExpr: Boolean): ComplexPath? {
		val segmentFunc = if (inExpr) ::segmentInExpr else ::segmentInType
		val segments = arrayListOf(segmentFunc(beginning) ?: return null)
		while (isPrevious(COLON_COLON)) segments.add(expect(IDENTIFIER)?.let { id -> segmentFunc(id) } ?: return null)
		return ComplexPath(segments)
	}

	private fun qualifiedPath(start: Token, inExpr: Boolean): QualifiedPath? {
		val qualifiedSegment = qualifiedSegment(start) ?: return null
		val segmentFunc = if (inExpr) ::segmentInExpr else ::segmentInType
		expect(COLON_COLON) ?: return null
		val segments = ArrayList<ComplexSegment>()
		while (isPrevious(COLON_COLON)) segments.add(expect(IDENTIFIER)?.let { id -> segmentFunc(id) } ?: return null)
		return QualifiedPath(qualifiedSegment, segments)
	}

	// utility
	private inline fun <T> group(closeType: TokenType, action: () -> T?, whileAction: () -> Boolean = { match(COMMA) }): Pair<List<T>, Token>? {
		if (match(closeType)) return Pair(emptyList(), previous)
		val list = ArrayList<T>()
		do {
			list.add(action() ?: return null)
		} while (whileAction())
		expect(closeType) ?: return null

		return Pair(list, previous)
	}

	private inline fun <T> trailingCommaGroup(closeType: TokenType, action: () -> T?) =
		group(closeType, action, { !isNext(closeType) && !match(COMMA) || !isNext(closeType) })

	private fun addToScope(scope: Scope, declaration: Stmt) {
		when (declaration) {
			is Stmt.Function -> scope.values[declaration.name] = Item.Function(declaration)
			is Stmt.Const -> scope.values[declaration.name] = Item.Constant(declaration)

			is Stmt.TupleStruct -> {
				val item = Item.TupleStruct(declaration)
				scope.values[declaration.name] = item
				scope.types[declaration.name] = item
			}

			is Stmt.UnitStruct -> {
				val item = Item.UnitStruct(declaration)
				scope.values[declaration.name] = item
				scope.types[declaration.name] = item
			}

			is Stmt.Trait -> scope.types[declaration.name] = Item.Trait(declaration, Scope.Branch(zinc), Scope.Branch(zinc))
			is Stmt.Struct -> scope.types[declaration.name] = Item.Struct(declaration)
			is Stmt.TypeAlias -> scope.types[declaration.name] = Item.TypeAlias(declaration)
			is Stmt.Enum -> scope.types[declaration.name] = Item.Enum(declaration, Scope.Branch(zinc), Scope.Branch(zinc))

			is Stmt.Implementation, is Stmt.Expression, is Stmt.Let -> {}
		}
	}

	// tokenization

	private fun consume(): Token {
		previous = current
		current = lexer.scanToken()
		return previous
	}

	private fun expect(type: TokenType) =
		if (!match(type)) {
			zinc.reportCompileError(CompilerError.unexpectedToken(current, type))
			null
		} else previous


	private fun match(type: TokenType) =
		if (isNext(type)) {
			consume()
			true
		} else false

	private fun atEnd() = isNext(EOF)

	private fun isNext(type: TokenType) = current.type == type

	private fun isPrevious(type: TokenType) = previous.type == type
}*/