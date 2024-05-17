package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.Precedence.*

internal data class Token(val type: Type, val line: Int, val range: IntRange, val lexeme: String = "") {
	val prec get() = type.rule.precedence

	companion object {
		fun empty(): Token {
			return Token(Type.NA, 0, 0..0)
		}
	}

	enum class Type(val rule: ParseRule = ParseRule(NONE, null, null)) {
		LEFT_PAREN(ParseRule(CALL, Compiler::parenthesis, Compiler::call)),
		RIGHT_PAREN,
		LEFT_BRACE(ParseRule(INIT, Compiler::block)),
		RIGHT_BRACE,
		LEFT_BRACKET,
		RIGHT_BRACKET,
		COMMA,
		DOT(ParseRule(CALL, infix = Compiler::dot)),
		PLUS(ParseRule(TERM, infix = Compiler::term)),
		PLUS_EQUAL,
		PLUS_PLUS,
		MINUS(ParseRule(TERM, Compiler::unary, Compiler::term)),
		MINUS_EQUAL,
		MINUS_MINUS,
		STAR(ParseRule(FACTOR, infix = Compiler::factor)),
		STAR_EQUAL,
		SLASH(ParseRule(FACTOR, infix = Compiler::factor)),
		SLASH_EQUAL,
		PERCENT(ParseRule(FACTOR, infix = Compiler::factor)),
		PERCENT_EQUAL,
		CARET(ParseRule(EXPONENT, infix = Compiler::exponent)),
		CARET_EQUAL,
		COLON,
		SEMICOLON,
		QUESTION,
		BANG(ParseRule(prefix = Compiler::unary)),
		BANG_EQUAL(ParseRule(EQUALITY, infix = Compiler::equality)),
		EQUAL,
		EQUAL_EQUAL(ParseRule(EQUALITY, infix = Compiler::equality)),
		GREATER(ParseRule(COMPARISON, infix = Compiler::comparison)),
		GREATER_EQUAL(ParseRule(COMPARISON, infix = Compiler::comparison)),
		LESS(ParseRule(COMPARISON, infix = Compiler::comparison)),
		LESS_EQUAL(ParseRule(COMPARISON, infix = Compiler::comparison)),
		IDENTIFIER(ParseRule(prefix = Compiler::variable)),
		STRING_VALUE(ParseRule(prefix = Compiler::stringLiteral)),
		CHAR_VALUE(ParseRule(prefix = Compiler::charLiteral)),
		NUMBER_VALUE(ParseRule(prefix = Compiler::numberLiteral)),
		TRUE(ParseRule(prefix = Compiler::trueLiteral)),
		FALSE(ParseRule(prefix = Compiler::falseLiteral)),
		STRUCT,
		SELF,
		DEF,
		VAR,
		VAL,
		PUB,
		INT,
		FOR,
		WHILE,
		LOOP,
		IF(ParseRule(prefix = Compiler::ternary)),
		ELSE,
		ELIF,
		RETURN(ParseRule(prefix = Compiler::returnExpr)),
		BREAK(ParseRule(prefix = Compiler::breakExpr)),
		AMP_AMP(ParseRule(AND, infix = Compiler::and)),
		PIPE_PIPE(ParseRule(OR, infix = Compiler::or)),
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