package com.light672.zinc.ast

import com.light672.zinc.CompilerError
import com.light672.zinc.ScopeInfo
import com.light672.zinc.ScopeInfo.Branch
import com.light672.zinc.Zinc
import com.light672.zinc.ast.TokenType.*
import com.light672.zinc.item.Implementation
import com.light672.zinc.item.TypeItem
import com.light672.zinc.item.ValueItem

internal class Parser(private val zinc: Zinc.Runtime) {
	private val lexer = Lexer(zinc.source, zinc)
	private var previous = Token.empty()
	private var current = lexer.scanToken()

	fun parse(scope: ScopeInfo) {
		while (!atEnd()) addItem(scope) ?: return
	}

	// declarations

	// only occurs at the module level
	private fun addItem(scope: ScopeInfo): Unit? {
		if (match(STRUCT)) return addStructItem(previous, scope)
		if (match(INTERFACE)) return addInterfaceItem(previous, scope)
		if (match(IMPL)) return addImplItem(previous, scope)
		if (match(FN)) return addFunctionItem(previous, scope.values, ValueItem.Function.ParentType.MODULE)
		zinc.reportCompileError(CompilerError.expectedDeclaration(current))
		return null
	}

	private fun addStructItem(keyword: Token, scope: ScopeInfo): Unit? {
		val name = expect(IDENTIFIER) ?: return null
		if (!isNext(SEMICOLON)) expect(LEFT_BRACE) ?: return null

		val (fields, close) = if (match(SEMICOLON)) Pair(emptyList(), previous) else trailingCommaGroup(RIGHT_PAREN) {
			val name = expect(IDENTIFIER) ?: return null
			expect(COLON) ?: return null
			val type = expectType() ?: return null
			Pair(name, type)
		} ?: return null

		val struct = TypeItem.Struct(name, fields)

		if (fields.isEmpty()) {
			scope.values.add(name, ValueItem.UnitStruct(struct))
		}

		return scope.types.add(name, struct)
	}

	private fun addInterfaceItem(keyword: Token, scope: ScopeInfo): Unit? {
		val name = expect(IDENTIFIER) ?: return null
		expect(LEFT_BRACE) ?: return null
		val values = Branch<ValueItem>(zinc)
		while (!isNext(RIGHT_BRACE)) {
			val keyword = expect(FN) ?: return null
			addFunctionItem(keyword, values, ValueItem.Function.ParentType.INTERFACE)
		}
		val close = expect(RIGHT_BRACE) ?: return null
		return scope.types.add(name, TypeItem.Interface(name, values))
	}

	private fun addImplItem(keyword: Token, scope: ScopeInfo): Unit? {
		val type = expectType() ?: return null
		val inheritedInterface = if (match(COLON)) {
			expectType() ?: return null
		} else null

		expect(LEFT_BRACE) ?: return null
		val values = Branch<ValueItem>(zinc)
		while (!isNext(RIGHT_BRACE)) {
			val keyword = expect(FN) ?: return null
			addFunctionItem(
				keyword,
				values,
				if (inheritedInterface != null)
					ValueItem.Function.ParentType.INHERIT_IMPL
				else
					ValueItem.Function.ParentType.IMPL
			)
		}
		val close = expect(RIGHT_BRACE) ?: return null

		scope.implementationItems.add(Implementation(type, inheritedInterface, values))
		return Unit
	}

	private fun addFunctionItem(keyword: Token, values: Branch<ValueItem>, parentType: ValueItem.Function.ParentType): Unit? {
		val name = expect(IDENTIFIER) ?: return null
		expect(LEFT_PAREN) ?: return null

		val (parameters, close) = trailingCommaGroup(RIGHT_PAREN) {
			val pattern = expectPattern() ?: return null
			expect(COLON) ?: return null
			val type = expectType() ?: return null
			Pair(pattern, type)
		} ?: return null

		val returnType = if (match(MINUS_ARROW)) {
			expectType() ?: return null
		} else null

		val block = expectBlock() ?: return null

		return values.add(name, ValueItem.Function(keyword, name, parameters, returnType, block, parentType))
	}

	// statements

	/**
	 * returns true if it declared an item, false if it did not, and null if there was an error
	 */
	private fun itemStatement(scope: ScopeInfo): Boolean? {
		if (match(STRUCT)) return addStructItem(previous, scope)?.let { true }
		if (match(INTERFACE)) return addInterfaceItem(previous, scope)?.let { true }
		if (match(IMPL)) return addImplItem(previous, scope)?.let { true }
		if (match(FN)) return addFunctionItem(previous, scope.values, ValueItem.Function.ParentType.MODULE)?.let { true }
		return false
	}

	private fun statement(): Stmt? {
		if (match(LET)) return letStatement(previous)
		return expressionStatement()
	}

	private fun letStatement(keyword: Token): Stmt.Let? {
		val pattern = expectPattern() ?: return null
		val type = if (match(COLON)) expectType() ?: return null else null
		val initializer = if (match(EQUAL)) expression() ?: return null else null
		expect(SEMICOLON) ?: return null
		return Stmt.Let(keyword, pattern, type, initializer)
	}

	private fun expressionStatement(): Stmt.Expression? {
		val expression = parsePrecedence(Precedence.ASSIGNMENT, CompilerError::expectedStatement) ?: return null
		val trailing = when (expression) {
			is Expr.Block -> !match(SEMICOLON)
			else -> isNext(RIGHT_BRACE) || run {
				expect(SEMICOLON) ?: return null
				false
			}
		}
		return Stmt.Expression(expression, trailing)
	}

	// expressions
	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT, CompilerError::expectedExpression)

	private fun parsePrecedence(
		precedence: Precedence,
		expected: (Token) -> CompilerError,
	): Expr? {
		val prefixToken = consume()
		val rule = prefixToken.type.prefix ?: run {
			zinc.reportCompileError(expected(previous))
			return null
		}
		var left = rule(prefixToken) ?: return null
		while (precedence.ordinal <= ((current as? Token)?.type?.precedence ?: Precedence.NONE).ordinal) {
			val infixOperator = consume()
			val infix = infixOperator.type.infix!!
			left = infix(left, infixOperator) ?: return null
		}
		return left
	}

	private fun binary(left: Expr, operator: Token, precedence: Precedence): Expr.Binary? {
		val right = parsePrecedence(precedence, CompilerError::expectedExpression) ?: return null
		return Expr.Binary(left, operator, right)
	}

	// prefix: NUMBER, STRING
	fun literal(token: Token): Expr {
		return Expr.Literal(token)
	}

	// prefix: IDENTIFIER
	fun path(start: Token): Expr? {
		val path = complexPath(start) ?: return null
		return Expr.Variable(path)
	}

	// prefix: '{'
	fun block(brace: Token): Expr.Block? {
		val stmts = ArrayList<Stmt>()
		val innerScope = ScopeInfo(zinc)
		while (!isNext(RIGHT_BRACE)) {
			if (itemStatement(innerScope) ?: return null) continue
			val statement = statement() ?: return null // TODO: error recovery is too much work
			stmts.add(statement)
		}
		val close = expect(RIGHT_BRACE) ?: return null
		return Expr.Block(stmts, innerScope)
	}

	private fun expectBlock(): Expr.Block? {
		if (!match(LEFT_BRACE)) {
			zinc.reportCompileError(CompilerError.expectedBlock(current))
			return null
		}
		return block(previous)
	}

	// prefix: '('
	fun group(paren: Token): Expr? {
		val (list, close) = group(RIGHT_PAREN, ::expression) ?: return null
		if (list.size == 1) return list[0]
		return Expr.Tuple(list)
	}

	// infix: '=', '+=', '-=', '*=', '/=', '|=', '&=', '^='
	fun assignment(left: Expr, operator: Token) =
		binary(left, operator, Precedence.ASSIGNMENT)

	// infix: '||'
	fun or(left: Expr, operator: Token) =
		binary(left, operator, Precedence.AND)

	// infix: '&&'
	fun and(left: Expr, operator: Token) =
		binary(left, operator, Precedence.EQUALITY)

	// infix: '==', '!='
	fun equality(left: Expr, operator: Token) =
		binary(left, operator, Precedence.COMPARISON)

	// infix: '<', '>', '<=', '>='
	fun comparison(left: Expr, operator: Token) =
		binary(left, operator, Precedence.BIT_OR)

	// infix: '|'
	fun bitOr(left: Expr, operator: Token) =
		binary(left, operator, Precedence.BIT_XOR)

	// infix: '^'
	fun bitXor(left: Expr, operator: Token) =
		binary(left, operator, Precedence.BIT_AND)

	// infix: '&'
	fun bitAnd(left: Expr, operator: Token) =
		binary(left, operator, Precedence.TERM)

	// infix: '+', '-'
	fun term(left: Expr, operator: Token) =
		binary(left, operator, Precedence.FACTOR)

	// infix: '*', '/'
	fun factor(left: Expr, operator: Token) =
		binary(left, operator, Precedence.UNARY)

	// infix: '-', '!', '~'
	fun unary(operator: Token): Expr.Unary? {
		val right = parsePrecedence(Precedence.UNARY, CompilerError::expectedExpression) ?: return null
		return Expr.Unary(operator, right)
	}

	// infix: '('
	fun call(callee: Expr, operator: Token): Expr? {
		val (arguments, close) = trailingCommaGroup(RIGHT_PAREN, ::expression) ?: return null
		return Expr.Call(callee, arguments)
	}


	// types
	private fun expectType(): Type? {
		if (match(LEFT_PAREN)) return tupleType(previous)
		if (match(LESS)) return qualifiedComplexPath(previous)
		if (match(IDENTIFIER)) return complexPath(previous)
		zinc.reportCompileError(CompilerError.expectedType(current))
		return null
	}

	private fun tupleType(start: Token): Type.Tuple? {
		val (list, end) = group(RIGHT_PAREN, ::expectType) ?: return null
		return Type.Tuple(start, list, end)
	}

	// patterns
	private fun expectPattern(): Pattern? {
		if (match(LEFT_PAREN)) return tuplePattern(previous)
		if (match(MUT)) return mutIdentifierPattern(previous)
		if (match(IDENTIFIER)) return pathPattern(previous)
		if (match(LESS)) return Pattern.TypePath(qualifiedComplexPath(previous) ?: return null)
		if (match(UNDERSCORE)) return Pattern.Underscore(previous)
		zinc.reportCompileError(CompilerError.expectedPattern(current))
		return null
	}

	private fun tuplePattern(start: Token): Pattern.Tuple? {
		val (fields, close) = group(RIGHT_PAREN, ::expectPattern) ?: return null
		return Pattern.Tuple(start, fields, close)
	}

	private fun pathPattern(start: Token): Pattern? {
		val path = complexPath(start) ?: return null
		if (match(LEFT_PAREN)) {
			val tuple = tuplePattern(previous) ?: return null
			return Pattern.TupleStruct(path, tuple.fields, tuple.close)
		}
		if (match(LEFT_BRACE)) {
			val (body, close) = trailingCommaGroup(RIGHT_BRACE) {
				val field = expect(IDENTIFIER) ?: return null
				val pattern = if (match(COLON)) {
					expectPattern() ?: return null
				} else null

				Pair(field, pattern)
			} ?: return null
			return Pattern.Struct(path, body, null, close)
		}
		if (path.body.size == 1) return Pattern.Identifier(null, start)
		return Pattern.TypePath(path)
	}

	private fun mutIdentifierPattern(mut: Token): Pattern? {
		val identifier = expect(IDENTIFIER) ?: return null
		return Pattern.Identifier(mut, identifier)
	}

	// paths

	private fun complexPath(start: Token): ComplexPath.Normal? {
		val body = ArrayList<ComplexPath.Segment>()
		body.add(typePathSegment(start))
		while (isPrevious(COLON_COLON)) {
			val start = expect(IDENTIFIER) ?: return null
			body.add(typePathSegment(start))
		}
		return ComplexPath.Normal(body)
	}

	private fun expectComplexPath(): ComplexPath? {
		if (match(IDENTIFIER)) return complexPath(previous)
		zinc.reportCompileError(CompilerError.expectedTypePath(current))
		return null
	}

	private fun qualifiedComplexPath(open: Token): ComplexPath? {
		val type = expectType() ?: return null
		expect(AS) ?: return null
		val cast = expectComplexPath() ?: return null
		val close = expect(GREATER) ?: return null
		match(COLON_COLON)
		val body = ArrayList<ComplexPath.Segment>()
		while (isPrevious(COLON_COLON)) {
			val start = expect(IDENTIFIER) ?: return null
			body.add(typePathSegment(start))
		}
		return ComplexPath.Qualified(
			ComplexPath.Qualified.QualifiedSegment(open, type, cast, close),
			body
		)
	}

	private fun typePathSegment(start: Token): ComplexPath.Segment {
		match(COLON_COLON)
		return ComplexPath.Segment(start)
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
}