package com.light672.zinc.lang.compiler.parsing.syntax

import com.light672.zinc.lang.compiler.parsing.Parser
import com.light672.zinc.lang.compiler.parsing.Precedence
import com.light672.zinc.lang.compiler.parsing.Precedence.*

internal data class Token(val type: Type, val line: Int, val range: IntRange, val lexeme: String = "") {
	val prec get() = type.rule.precedence

	companion object {
		fun empty(): Token {
			return Token(Type.NA, 0, 0..0)
		}
	}

	internal class ParseRule(
		val precedence: Precedence = NONE,
		val prefix: (Parser.() -> Expr)? = null,
		val infix: (Parser.(Expr) -> Expr)? = null
	)


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
		CARET(ParseRule(EXPONENT, infix = Parser::exponent)),
		CARET_EQUAL,
		COLON,
		COLON_EQUAL,
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
		DEF,
		PUB,
		MUT(ParseRule(prefix = Parser::mutReference)),
		FOR,
		WHILE,
		IF,
		ELSE,
		ELIF,
		RETURN,
		BREAK,
		AMP_AMP(ParseRule(AND, infix = Parser::and)),
		PIPE_PIPE(ParseRule(OR, infix = Parser::or)),
		IS,
		AS,
		IN,
		TRAIT,
		IMPL,
		EOF,
		ERROR,
		NA
	}

	override fun toString(): String {
		return "Token($type, $lexeme)"
	}
}