package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Token

internal sealed class CompilerError(val message: String) {
	class SimpleError(message: String) : CompilerError(message)
	class TokenError(val token: Token, message: String) : CompilerError(message)

	class OneRangeError(val range: IntRange, message: String) : CompilerError(message)

	class TwoRangeError(
		val rangeA: IntRange,
		val rangeB: IntRange,
		val rangeAMessage: String,
		val rangeBMessage: String,
		message: String
	) : CompilerError(message)
}