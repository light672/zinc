package com.light672.zinc.ast

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.TokenType.EOF


internal sealed interface ParseResult<out T> {
	data class Success<out T>(val value: T) : ParseResult<T>
	data object Error : ParseResult<Nothing>
	data class NoMatch(val error: CompilerError) : ParseResult<Nothing>

	operator fun unaryPlus() = unwrap()
	fun unwrap() = when (this) {
		is Success -> value
		else -> throw IllegalArgumentException()
	}

	fun isSuccess() = this is Success
}

internal class CombinatorParser(private val zinc: Zinc.Runtime) {
	private val lexer = Lexer(zinc.source, zinc)
	var previous = Token.empty()
	var current = lexer.scanToken()

	fun <T> success(value: T) = ParseResult.Success(value)
	fun failure() = ParseResult.Error
	fun noMatch(error: CompilerError) = ParseResult.NoMatch(error)

	fun <T> optional(result: ParseResult<T>): ParseResult<T?> {
		return when (result) {
			is ParseResult.Error -> result
			is ParseResult.NoMatch -> ParseResult.Success(null)
			is ParseResult.Success -> result
		}
	}

	inline infix fun <T> ParseResult<T>.or(parser: () -> ParseResult<T>): ParseResult<T> {
		return when (this) {
			ParseResult.Error -> this
			is ParseResult.NoMatch -> parser()
			is ParseResult.Success -> this
		}
	}

	fun <T> expect(result: ParseResult<T>): ParseResult<T> {
		return when (result) {
			ParseResult.Error, is ParseResult.Success -> result
			is ParseResult.NoMatch -> {
				zinc.reportCompileError(result.error)
				ParseResult.Error
			}
		}
	}

	fun token(type: TokenType): ParseResult<Token> {
		return if (consumeIfMatch(type)) ParseResult.Success(previous)
		else ParseResult.NoMatch(CompilerError.unexpectedToken(current, type))
	}

	fun token(vararg types: TokenType): ParseResult<Token> {
		return if (consumeIfMatch(types)) ParseResult.Success(previous)
		else ParseResult.NoMatch(CompilerError.unexpectedToken(current, types))
	}

	inline fun <T, R> ParseResult<T>.map(transform: (T) -> R) = map(this, transform)

	@JvmName("mapFunc")
	inline fun <T, R> map(result: ParseResult<T>, transform: (T) -> R): ParseResult<R> {
		return when (result) {
			is ParseResult.Error -> result
			is ParseResult.NoMatch -> result
			is ParseResult.Success -> ParseResult.Success(transform(result.value))
		}
	}

	inline fun <T, R> ParseResult<T>.flatMap(transform: (T) -> ParseResult<R>): ParseResult<R> {
		return when (val result = this) {
			is ParseResult.Error -> result
			is ParseResult.NoMatch -> result
			is ParseResult.Success -> transform(result.value)
		}
	}

	inline fun <T, R> ParseResult<T>.then(parser: () -> ParseResult<R>): ParseResult<R> = flatMap { _ -> parser() }

	inline fun <T> ParseResult<T>.with(action: (T) -> Unit): ParseResult<T> = with(this, action)

	@JvmName("withFunc")
	inline fun <T> with(result: ParseResult<T>, action: (T) -> Unit) = map(result) { r -> action(r); r }

	inline fun <T> many(parser: () -> ParseResult<T>, existing: List<T>? = null): ParseResult<List<T>> {
		val list = existing?.toMutableList() ?: ArrayList()
		while (true) {
			when (val result = parser()) {
				is ParseResult.Error -> return result
				is ParseResult.NoMatch -> return ParseResult.Success(list)
				is ParseResult.Success -> list.add(result.value)
			}
		}
		throw IllegalArgumentException()
	}

	inline fun <T> loop(parser: () -> ParseResult<T>): ParseResult<T> {
		var previous = parser()
		while (true) {
			when (previous) {
				ParseResult.Error -> return ParseResult.Error
				is ParseResult.NoMatch -> return previous
				is ParseResult.Success -> when (val b = parser()) {
					ParseResult.Error -> return ParseResult.Error
					is ParseResult.NoMatch -> return previous
					is ParseResult.Success -> previous = b
				}
			}
		}
	}

	inline fun <T> manyUntil(parser: () -> ParseResult<T>, end: TokenType): ParseResult<Pair<List<T>, Token>> {
		if (consumeIfMatch(end)) return success(Pair(emptyList(), previous))
		val list = ArrayList<T>()
		do {
			list.add(
				when (val result = expect(parser())) {
					is ParseResult.Error -> return result
					is ParseResult.NoMatch -> return result
					is ParseResult.Success -> result.value
				}
			)
		} while (!isNext(end))
		expect(end) ?: return ParseResult.Error
		return ParseResult.Success(Pair(list, previous))
	}

	inline fun <T> manySeparatedUntil(parser: () -> ParseResult<T>, separator: TokenType, end: TokenType): ParseResult<Pair<List<T>, Token>> {
		if (consumeIfMatch(end)) return ParseResult.Success(Pair(emptyList<T>(), previous))
		val list = ArrayList<T>()
		do {
			list.add(
				when (val result = expect(parser())) {
					is ParseResult.Error -> return result
					is ParseResult.NoMatch -> return result
					is ParseResult.Success -> result.value
				}
			)
		} while (consumeIfMatch(separator))
		expect(separator, end) ?: return ParseResult.Error
		return ParseResult.Success(Pair(list, previous))
	}

	inline fun <T> manyTrailingUntil(
		parser: () -> ParseResult<T>,
		separator: TokenType,
		end: TokenType
	): ParseResult<Pair<List<T>, Token>> {
		if (consumeIfMatch(end)) return ParseResult.Success(Pair(emptyList(), previous))
		val list = ArrayList<T>()
		do {
			list.add(
				when (val result = expect(parser())) {
					is ParseResult.Error -> return result
					is ParseResult.NoMatch -> return result
					is ParseResult.Success -> result.value
				}
			)
		} while (consumeIfMatch(separator) && !isNext(end))
		expect(separator, end) ?: return ParseResult.Error
		return ParseResult.Success(Pair(list, previous))
	}

	fun <T> ParseResult<T>.error(createError: () -> CompilerError): ParseResult<T> {
		return when (this) {
			ParseResult.Error -> this
			is ParseResult.NoMatch -> ParseResult.NoMatch(createError())
			is ParseResult.Success -> this
		}
	}


	// tokenization
	private fun consume(): Token {
		previous = current
		current = lexer.scanToken()
		return previous
	}

	private fun expect(type: TokenType) =
		if (!consumeIfMatch(type)) {
			zinc.reportCompileError(CompilerError.unexpectedToken(current, type))
			null
		} else previous

	private fun expect(vararg types: TokenType) =
		if (!consumeIfMatch(types)) {
			zinc.reportCompileError(CompilerError.unexpectedToken(current, types))
			null
		} else previous


	private fun consumeIfMatch(type: TokenType) =
		if (isNext(type)) {
			consume()
			true
		} else false

	private fun consumeIfMatch(types: Array<out TokenType>) =
		if (isNext(types)) {
			consume()
			true
		} else false


	private fun atEnd() = isNext(EOF)

	fun isNext(type: TokenType) = current.type == type
	private fun isNext(types: Array<out TokenType>) = current.type in types
	private fun isPrevious(type: TokenType) = previous.type == type
}