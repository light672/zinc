package com.light672.zinc.lang.compiler.ast.syntax

import com.light672.zinc.lang.compiler.ast.Parser
import com.light672.zinc.lang.compiler.ast.Precedence
import com.light672.zinc.lang.compiler.ast.Precedence.*

internal data class Token(val type: Type, val line: Int, val range: IntRange, val lexeme: String = "") {
	companion object {
		fun empty() = Token(Type.NA, 0, 0..0)

		fun newNA(lexeme: String, line: Int, range: IntRange) = Token(Type.NA, line, range, lexeme)

	}

	internal class ParseRule(
		val precedence: Precedence = NONE,
		val prefix: (Parser.() -> Expr)? = null,
		val infix: (Parser.(Expr) -> Expr)? = null
	)

	class Range(val first: Token, val last: Token)

	operator fun rangeTo(token: Token) = Range(this, token)
	fun asRange() = Range(this, this)

	enum class Type(val rule: ParseRule = ParseRule(NONE, null, null)) {
		LEFT_PAREN(ParseRule(CALL, Parser::parenthesis)),
		RIGHT_PAREN,
		LEFT_BRACE(ParseRule(prefix = Parser::block)),
		RIGHT_BRACE,
		LEFT_BRACKET,
		RIGHT_BRACKET,
		COMMA,
		DOT(ParseRule(CALL)),
		PLUS(ParseRule(TERM, infix = Parser::term)),
		PLUS_EQUAL,
		PLUS_PLUS,
		MINUS(ParseRule(TERM, Parser::unary, Parser::term)),
		MINUS_EQUAL,
		MINUS_MINUS,
		STAR(ParseRule(FACTOR, infix = Parser::factor)),
		STAR_EQUAL,
		SLASH(ParseRule(FACTOR, infix = Parser::factor)),
		SLASH_EQUAL,
		PERCENT(ParseRule(FACTOR, infix = Parser::factor)),
		PERCENT_EQUAL,
		CARET,
		CARET_EQUAL,
		COLON,
		COLON_COLON,
		SEMICOLON,
		QUESTION,
		BANG(ParseRule(prefix = Parser::unary)),
		BANG_EQUAL(ParseRule(EQUALITY, infix = Parser::equality)),
		EQUAL,
		EQUAL_EQUAL(ParseRule(EQUALITY, infix = Parser::equality)),
		GREATER(ParseRule(COMPARISON, infix = Parser::comparison)),
		GREATER_EQUAL(ParseRule(COMPARISON, infix = Parser::comparison)),
		LESS(ParseRule(COMPARISON, infix = Parser::comparison)),
		LESS_EQUAL(ParseRule(COMPARISON, infix = Parser::comparison)),
		IDENTIFIER(ParseRule(prefix = Parser::variable)),
		STRING_VALUE(ParseRule(prefix = Parser::stringLiteral)),
		CHAR_VALUE(ParseRule(prefix = Parser::charLiteral)),
		NUMBER_VALUE(ParseRule(prefix = Parser::numberLiteral)),
		TRUE(ParseRule(prefix = Parser::trueLiteral)),
		FALSE(ParseRule(prefix = Parser::falseLiteral)),
		STRUCT,
		SELF,
		FN,
		PUB,
		LET,
		MUT,
		FOR,
		WHILE,
		IF,
		ELSE,
		ELIF,
		RETURN,
		BREAK,
		AMP,
		PIPE,
		AMP_AMP(ParseRule(AND, infix = Parser::and)),
		PIPE_PIPE(ParseRule(OR, infix = Parser::or)),
		DOLLAR,
		AT,
		IS,
		AS,
		IN,
		INTERFACE,
		IMPL,
		EOF,
		ERROR,
		NA
	}

	override fun toString(): String {
		return "Token($type, $lexeme)"
	}
}