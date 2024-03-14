package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.lang.compiler.parsing.Precedence.*

internal data class Token(val type: Type, val line: Int, val range: IntRange, val lexeme: String = "") {
	val prec get() = type.rule.precedence

	companion object {
		fun empty(): Token {
			return Token(Type.NA, 0, 0..0)
		}
	}

	enum class Type(val rule: ParseRule = ParseRule(NONE, null, null)) {
		LEFT_PAREN(ParseRule(CALL, PrattParser::grouping, PrattParser::call)),
		RIGHT_PAREN,
		LEFT_BRACE(ParseRule(INIT, infix = PrattParser::init)),
		RIGHT_BRACE,
		LEFT_BRACKET,
		RIGHT_BRACKET,
		COMMA,
		DOT(ParseRule(CALL, infix = PrattParser::dot)),
		PLUS(ParseRule(TERM, infix = PrattParser::term)),
		PLUS_EQUAL,
		PLUS_PLUS,
		MINUS(ParseRule(TERM, PrattParser::unary, PrattParser::term)),
		MINUS_EQUAL,
		MINUS_MINUS,
		STAR(ParseRule(FACTOR, infix = PrattParser::factor)),
		STAR_EQUAL,
		SLASH(ParseRule(FACTOR, infix = PrattParser::factor)),
		SLASH_EQUAL,
		PERCENT(ParseRule(FACTOR, infix = PrattParser::factor)),
		PERCENT_EQUAL,
		CARET(ParseRule(EXPONENT, infix = PrattParser::exponent)),
		CARET_EQUAL,
		COLON,
		SEMICOLON,
		QUESTION,
		BANG(ParseRule(prefix = PrattParser::unary)),
		BANG_EQUAL(ParseRule(EQUALITY, infix = PrattParser::equality)),
		EQUAL,
		EQUAL_EQUAL(ParseRule(EQUALITY, infix = PrattParser::equality)),
		GREATER(ParseRule(COMPARISON, infix = PrattParser::comparison)),
		GREATER_EQUAL(ParseRule(COMPARISON, infix = PrattParser::comparison)),
		LESS(ParseRule(COMPARISON, infix = PrattParser::comparison)),
		LESS_EQUAL(ParseRule(COMPARISON, infix = PrattParser::comparison)),
		IDENTIFIER(ParseRule(prefix = PrattParser::variable)),
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
		RETURN(ParseRule(prefix = PrattParser::returnExpr)),
		BREAK(ParseRule(prefix = PrattParser::breakExpr)),
		AND(ParseRule(Precedence.AND, infix = PrattParser::and)),
		OR(ParseRule(Precedence.OR, infix = PrattParser::or)),
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