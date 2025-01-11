package com.light672.zinc.ast

internal class Token(
	val type: TokenType,
	val lexeme: CharSequence?,
	val line: Int,
	val rangeOnLine: IntRange
) {
	companion object {
		fun empty(line: Int = 0, rangeOnLine: IntRange = 0..0) = Token(TokenType.NA, "(EMPTY TOKEN)", line, rangeOnLine)
	}

	data class Range(val start: Token, val end: Token)

	operator fun rangeTo(token: Token) = Range(this, token)
	fun asRange() = this..this

	override fun toString() = lexeme?.toString() ?: type.asString
}

internal enum class TokenType(
	val asString: String,
) {
	FN("fn"),
	AS("as"),
	IF("if"),
	IN("in"),
	FOR("for"),
	LET("let"),
	MUT("mut"),
	MOD("mod"),
	ELSE("else"),
	LOOP("loop"),
	IMPL("impl"),
	SELF("self"),
	ENUM("enum"),
	TRUE("true"),
	FALSE("false"),
	WHILE("while"),
	MATCH("match"),
	BREAK("break"),
	CONST("const"),
	RETURN("return"),
	STRUCT("struct"),
	INTERFACE("interface"),
	TYPEALIAS("typealias"),
	UNDERSCORE("_"),
	SEMICOLON(";"),
	LEFT_PAREN("("),
	RIGHT_PAREN(")"),
	LEFT_BRACE("{"),
	RIGHT_BRACE("}"),
	LEFT_BRACKET("["),
	RIGHT_BRACKET("]"),
	EQUAL("="),
	COLON(":"),
	COMMA(","),
	DOT("."),
	PLUS("+"),
	MINUS("-"),
	STAR("*"),
	SLASH("/"),
	PERCENT("%"),
	LESS("<"),
	GREATER(">"),
	AMP("&"),
	PIPE("|"),
	CARET("^"),
	BANG("!"),
	TILDA("~"),
	EQUAL_EQUAL("=="),
	LESS_EQUAL("<="),
	GREATER_EQUAL(">="),
	AMP_AMP("&&"),
	PIPE_PIPE("||"),
	PLUS_EQUAL("+="),
	MINUS_EQUAL("-="),
	STAR_EQUAL("*="),
	SLASH_EQUAL("/="),
	PERCENT_EQUAL("%="),
	CARET_EQUAL("^="),
	PIPE_EQUAL("|="),
	AMP_EQUAL("&="),
	PLUS_PLUS("++"),
	MINUS_MINUS("--"),
	BANG_EQUAL("!="),
	COLON_COLON("::"),
	DOT_DOT(".."),
	MINUS_ARROW("->"),
	EQUALS_ARROW("=>"),
	IDENTIFIER("IDENTIFIER"),
	NUMBER("NUMBER"),
	STRING("STRING"),
	NA("NA"),
	ERROR("ERROR"),
	EOF("EOF");

	override fun toString() = asString
}