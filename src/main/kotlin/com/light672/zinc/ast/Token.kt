package com.light672.zinc.ast

import com.light672.zinc.ast.Precedence.*

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
	val precedence: Precedence = NONE,
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
	LEFT_PAREN("(", CALL),
	RIGHT_PAREN(")"),
	LEFT_BRACE("{"),
	RIGHT_BRACE("}"),
	LEFT_BRACKET("["),
	RIGHT_BRACKET("]"),
	EQUAL("=", ASSIGNMENT),
	COLON(":"),
	COMMA(","),
	DOT("."),
	PLUS("+", TERM),
	MINUS("-", TERM),
	STAR("*", FACTOR),
	SLASH("/", FACTOR),
	PERCENT("%", FACTOR),
	LESS("<", COMPARISON),
	GREATER(">", COMPARISON),
	AMP("&", BIT_AND),
	PIPE("|", BIT_OR),
	CARET("^", BIT_XOR),
	BANG("!", UNARY),
	TILDA("~", UNARY),
	EQUAL_EQUAL("==", EQUALITY),
	LESS_EQUAL("<=", COMPARISON),
	GREATER_EQUAL(">=", COMPARISON),
	AMP_AMP("&&", AND),
	PIPE_PIPE("||", OR),
	PLUS_EQUAL("+=", ASSIGNMENT),
	MINUS_EQUAL("-=", ASSIGNMENT),
	STAR_EQUAL("*=", ASSIGNMENT),
	SLASH_EQUAL("/=", ASSIGNMENT),
	PERCENT_EQUAL("%=", ASSIGNMENT),
	CARET_EQUAL("^=", ASSIGNMENT),
	PIPE_EQUAL("|=", ASSIGNMENT),
	AMP_EQUAL("&=", ASSIGNMENT),
	PLUS_PLUS("++"),
	MINUS_MINUS("--"),
	BANG_EQUAL("!=", COMPARISON),
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