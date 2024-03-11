package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.lang.compiler.parsing.PrattParser.ParseRule
import com.light672.zinc.lang.compiler.parsing.PrattParser.Precedence.NONE
import com.light672.zinc.lang.compiler.parsing.PrattParser.Precedence.TERM

internal data class Token(val type: Type, val line: Int, val range: IntRange, val lexeme: String = "") {
	companion object {
		fun empty(): Token {
			return Token(Type.NA, 0, 0..0)
		}
	}

	enum class Type(val rule: ParseRule = ParseRule(NONE, null, null)) {
		LEFT_PAREN,
		RIGHT_PAREN,
		LEFT_BRACE,
		RIGHT_BRACE,
		LEFT_BRACKET,
		RIGHT_BRACKET,
		COMMA,
		DOT,
		PLUS,
		PLUS_EQUAL,
		PLUS_PLUS,
		MINUS(ParseRule(TERM, prefix = PrattParser::unary)),
		MINUS_EQUAL,
		MINUS_MINUS,
		STAR,
		STAR_EQUAL,
		SLASH,
		SLASH_EQUAL,
		PERCENT,
		PERCENT_EQUAL,
		CARET,
		CARET_EQUAL,
		COLON,
		SEMICOLON,
		QUESTION,
		BANG(ParseRule(prefix = PrattParser::unary)),
		BANG_EQUAL,
		EQUAL,
		EQUAL_EQUAL,
		GREATER,
		GREATER_EQUAL,
		LESS,
		LESS_EQUAL,
		IDENTIFIER,
		STRING_VALUE(ParseRule(prefix = PrattParser::stringLiteral)),
		CHAR_VALUE(ParseRule(prefix = PrattParser::charLiteral)),
		NUMBER_VALUE(ParseRule(prefix = PrattParser::numberLiteral)),
		TRUE(ParseRule(prefix = PrattParser::trueLiteral)),
		FALSE(ParseRule(prefix = PrattParser::falseLiteral)),
		STRUCT,
		FUNC,
		VAR,
		VAL,
		PUB,
		INT,
		FOR,
		WHILE,
		LOOP,
		IF,
		ELSE,
		ELIF,
		RETURN,
		BREAK,
		AND,
		OR,
		IS,
		AS,
		IN,
		EOF,
		ERROR,
		NA
	}

	override fun toString(): String {
		return "Token($type, $lexeme)"
	}
}