package zinc.lang.compiler

internal sealed class CompilerError(val range: IntRange, val message: String) {
	class TokenError(val token: Token, message: String) : CompilerError(token.range, message)

	class OneRangeError(range: IntRange, message: String) : CompilerError(range, message)

	class TwoRangeError(
		val rangeA: IntRange,
		rangeB: IntRange,
		val rangeAMessage: String,
		val rangeBMessage: String,
		message: String
	) : CompilerError(rangeB, message)
}