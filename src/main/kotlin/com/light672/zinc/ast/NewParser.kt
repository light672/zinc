package com.light672.zinc.ast

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.TokenType.*
import com.light672.zinc.tuple.*

internal class NewParser(val zinc: Zinc.Runtime) {

	// declarations


	// new expression
	private fun expr(): SingleParser<Expr> = assign()

	private fun assign(): SingleParser<Expr> = bin(EQUAL, leftP = or(), rightP = assign())
	private fun or() = bin(PIPE_PIPE, leftP = and())
	private fun and() = bin(AMP_AMP, leftP = equality())
	private fun equality() = bin(EQUAL_EQUAL, BANG_EQUAL, leftP = comp())
	private fun comp() = bin(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, leftP = term())
	private fun term() = bin(PLUS, MINUS, leftP = factor())
	private fun factor() = bin(STAR, SLASH_EQUAL, leftP = unary())
	private fun unary(): SingleParser<Expr> =
		match(BANG, MINUS, parser = unary()).map { tuple -> tuple?.let { (op, right) -> Expr.Unary(op, right) } }.orElse(call())

	private fun call(): SingleParser<Expr> = primary().then(optional(trailingCommaGroup(LEFT_PAREN, expr(), RIGHT_PAREN)))

	private fun <T> trailingCommaGroup(open: TokenType, parser: SingleParser<T>, close: TokenType): SingleParser<Triple<Token, T, Token>> =
		token(open).then(parser).then(token(close))

	private fun bin(vararg operators: TokenType, leftP: SingleParser<Expr>, rightP: SingleParser<Expr> = leftP): SingleParser<Expr> =
		leftP
			.then(match(types = operators, rightP))
			.map { (left, tuple) -> tuple?.let { (op, right) -> Expr.Binary(left, op, right) } ?: left }


	private val lexer = Lexer(zinc.source, zinc)
	private var previous = Token.empty()
	private var current = lexer.scanToken()


	data class SingleParser<out T>(val startCondition: () -> Boolean, val action: () -> ParseResult<T>)
	sealed interface ParseResult<out T> {
		class Success<out T>(val value: T) : ParseResult<T>
		class Failure() : ParseResult<Nothing>
	}

	private fun token(type: TokenType) = SingleParser({ isNext(type) }) {
		expect(type)?.let { t -> ParseResult.Success(t) } ?: ParseResult.Failure()
	}

	private fun tokensNoError(vararg types: TokenType) = SingleParser({ isNext(types = types) }) {
		expectNoError(types = types)?.let { t -> ParseResult.Success(t) } ?: ParseResult.Failure()
	}

	private fun <T> optional(parser: SingleParser<T>): SingleParser<T?> = SingleParser(parser.startCondition) {
		if (parser.startCondition()) parser.action()
		else ParseResult.Success<T?>(null)
	}

	private fun <T> match(type: TokenType, parser: SingleParser<T>) = optional(token(type)).then(parser)
	private fun <T> match(vararg types: TokenType, parser: SingleParser<T>) = optional(tokensNoError(types = types).then(parser))
	private fun <T1, T2> SingleParser<T1>.then(parser: SingleParser<T2>) =
		flatMap { a -> parser.map { b -> Tuple2(a, b) } }

	private fun <T1, T2, T3> SingleParser<Tuple2<T1, T2>>.then(parser: SingleParser<T3>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }

	private fun <T1, T2, T3, T4> SingleParser<Tuple3<T1, T2, T3>>.then(parser: SingleParser<T4>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }

	private fun <T1, T2, T3, T4, T5> SingleParser<Tuple4<T1, T2, T3, T4>>.then(parser: SingleParser<T5>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }

	private fun <T1, T2, T3, T4, T5, T6> SingleParser<Tuple5<T1, T2, T3, T4, T5>>.then(parser: SingleParser<T6>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }

	private fun <T1, T2, T3, T4, T5, T6, T7> SingleParser<Tuple6<T1, T2, T3, T4, T5, T6>>.then(parser: SingleParser<T7>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }

	private fun <T1, T2, T3, T4, T5, T6, T7, T8> SingleParser<Tuple7<T1, T2, T3, T4, T5, T6, T7>>.then(parser: SingleParser<T8>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }

	private fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> SingleParser<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>.then(parser: SingleParser<T9>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }

	private fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> SingleParser<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>.then(parser: SingleParser<T10>) =
		flatMap { a -> parser.map { b -> a.expand(b) } }


	private fun <T> SingleParser<T?>.or(parser: SingleParser<T?>): SingleParser<T?> = optional(this).orElse(parser)

	private fun <T> SingleParser<T?>.orElse(parser: SingleParser<T>): SingleParser<T> =
		SingleParser({ startCondition() || parser.startCondition() }) {
			when (val result = action()) {
				is ParseResult.Failure -> result
				is ParseResult.Success -> {
					if (result.value != null) ParseResult.Success(result.value)
					else parser.action()
				}
			}
		}

	private fun <A, B> SingleParser<A>.map(transform: (A) -> B): SingleParser<B> = SingleParser(startCondition) {
		when (val result = action()) {
			is ParseResult.Failure -> result
			is ParseResult.Success -> ParseResult.Success(transform(result.value))
		}
	}

	private fun <A, B> SingleParser<A>.flatMap(transform: (A) -> SingleParser<B>): SingleParser<B> = SingleParser(startCondition) {
		when (val result = action()) {
			is ParseResult.Failure -> result
			is ParseResult.Success -> transform(result.value).action()
		}
	}

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

	private fun expectNoError(vararg types: TokenType) =
		if (!match(types = types)) {
			// this error should never occur anyway, so it is safe to comment
			// zinc.reportCompileError(CompilerError.unexpectedToken(current, type))
			null
		} else previous


	private fun match(type: TokenType) =
		if (isNext(type)) {
			consume()
			true
		} else false

	private fun match(vararg types: TokenType) =
		if (isNext(types = types)) {
			consume()
			true
		} else false

	private fun atEnd() = isNext(EOF)

	private fun isNext(type: TokenType) = current.type == type
	private fun isNext(vararg types: TokenType) = current.type in types

	private fun isPrevious(type: TokenType) = previous.type == type
}