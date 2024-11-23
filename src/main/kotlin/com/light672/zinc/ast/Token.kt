package com.light672.zinc.ast

import com.light672.zinc.ast.Precedence.*

internal class Token(
	val type: TokenType,
	val lexeme: CharSequence,
	val line: Int,
	val rangeOnLine: IntRange
) {
	companion object {
		fun empty() = Token(TokenType.NA, "(EMPTY TOKEN)", 0, 0..0)
		fun withName(name: String) = Token(TokenType.NA, name, 0, 0..0)
	}

	operator fun rangeTo(token: Token) = Pair(line..token.line, rangeOnLine.first..token.rangeOnLine.last)
}

internal enum class TokenType(
	val precedence: Precedence = NONE,
	val prefix: (Parser.(Token) -> Expr?)? = null,
	val infix: (Parser.(Expr, Token) -> Expr?)? = null
) {
	FN,
	AS,
	LET,
	MUT,
	IMPL,
	RETURN,
	STRUCT,
	INTERFACE,
	UNDERSCORE,
	SEMICOLON,
	LEFT_PAREN(CALL, Parser::group, Parser::call),
	RIGHT_PAREN,
	LEFT_BRACE(prefix = Parser::block),
	RIGHT_BRACE,
	EQUAL(ASSIGNMENT, infix = Parser::assignment),
	COLON,
	COMMA,
	DOT,
	PLUS(TERM, infix = Parser::term),
	MINUS(TERM, Parser::unary, Parser::term),
	STAR(FACTOR, infix = Parser::factor),
	SLASH(FACTOR, infix = Parser::factor),
	LESS(COMPARISON, infix = Parser::comparison),
	GREATER(COMPARISON, infix = Parser::comparison),
	AMP(BIT_AND, infix = Parser::bitAnd),
	PIPE(BIT_OR, infix = Parser::bitOr),
	CARET(BIT_XOR, infix = Parser::bitXor),
	BANG(UNARY, prefix = Parser::unary),
	TILDA(UNARY, prefix = Parser::unary),
	EQUAL_EQUAL(EQUALITY, infix = Parser::equality),
	LESS_EQUAL(COMPARISON, infix = Parser::comparison),
	GREATER_EQUAL(COMPARISON, infix = Parser::comparison),
	AMP_AMP(AND, infix = Parser::and),
	PIPE_PIPE(OR, infix = Parser::or),
	PLUS_EQUAL(ASSIGNMENT, infix = Parser::assignment),
	MINUS_EQUAL(ASSIGNMENT, infix = Parser::assignment),
	STAR_EQUAL(ASSIGNMENT, infix = Parser::assignment),
	SLASH_EQUAL(ASSIGNMENT, infix = Parser::assignment),
	CARET_EQUAL(ASSIGNMENT, infix = Parser::assignment),
	PIPE_EQUAL(ASSIGNMENT, infix = Parser::assignment),
	AMP_EQUAL(ASSIGNMENT, infix = Parser::assignment),
	PLUS_PLUS,
	BANG_EQUAL(COMPARISON, infix = Parser::comparison),
	MINUS_MINUS,
	COLON_COLON,
	DOT_DOT,
	MINUS_ARROW,
	EQUALS_ARROW,
	IDENTIFIER(prefix = Parser::path),
	NUMBER(prefix = Parser::literal),
	STRING(prefix = Parser::literal),
	NA,
	ERROR,
	EOF
}