package com.light672.zinc.ast

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.TokenType.*

internal class Lexer(private val source: String, private val zinc: Zinc.Runtime) {

	private var start = 0
	private var current = 0
	private var startOnLine = 0
	private var currentOnLine = 0
	private var line = 1
	private val char get() = if (atEnd()) '\u0000' else source[current]
	private val previous get() = if (atBeginning()) '\u0000' else source[current - 1]
	private val next get() = if (current + 1 >= source.length) '\u0000' else source[current + 1]

	fun scanTokens(): List<Token> {
		val tokens = ArrayList<Token>()
		do {
			val t = scanToken()
			tokens.add(t)
		} while (t.type != EOF)
		return tokens
	}

	fun scanToken(): Token {
		skipWhiteSpace()
		if (atEnd()) return Token(EOF, "EOF", line, startOnLine..currentOnLine)
		return when (consume()) {
			in '0'..'9' -> number()
			in 'a'..'z', in 'A'..'Z', '_' -> keyword()

			'"' -> string()
			'#' -> {
				while (char != '\n' && !atEnd()) consume()
				scanToken()
			}

			';' -> create(SEMICOLON)
			'(' -> create(LEFT_PAREN)
			')' -> create(RIGHT_PAREN)
			'{' -> create(LEFT_BRACE)
			'}' -> create(RIGHT_BRACE)
			'[' -> create(LEFT_BRACKET)
			']' -> create(RIGHT_BRACKET)
			',' -> create(COMMA)
			'~' -> create(TILDA)
			':' -> create(normalOrDouble(COLON, COLON_COLON))
			'.' -> create(normalOrDouble(DOT, DOT_DOT))
			'=' -> create(if (match('>')) EQUALS_ARROW else normalOrEqual(EQUAL, EQUAL_EQUAL))
			'^' -> create(normalOrEqual(CARET, CARET_EQUAL))
			'!' -> create(normalOrEqual(BANG, BANG_EQUAL))
			'*' -> create(normalOrEqual(STAR, STAR_EQUAL))
			'/' -> create(normalOrEqual(SLASH, SLASH_EQUAL))
			'%' -> create(normalOrEqual(PERCENT, PERCENT_EQUAL))
			'<' -> create(normalOrEqual(LESS, LESS_EQUAL))
			'>' -> create(normalOrEqual(GREATER, GREATER_EQUAL))
			'+' -> create(normalDoubleOrEqual(PLUS, PLUS_PLUS, PLUS_EQUAL))
			'-' -> create(if (match('>')) MINUS_ARROW else normalDoubleOrEqual(MINUS, MINUS_MINUS, MINUS_EQUAL))
			'&' -> create(normalDoubleOrEqual(AMP, AMP_AMP, AMP_EQUAL))
			'|' -> create(normalDoubleOrEqual(PIPE, PIPE_PIPE, PIPE_EQUAL))

			else -> error(CompilerError.unexpectedChar(char, currentOnLine, line))
		}
	}

	private fun normalOrDouble(normal: TokenType, double: TokenType) = if (match(previous)) double else normal
	private fun normalOrEqual(normal: TokenType, equal: TokenType) = if (match('=')) equal else normal
	private fun normalDoubleOrEqual(normal: TokenType, double: TokenType, equal: TokenType) =
		if (match(previous)) double else normalOrEqual(normal, equal)

	private fun number(): Token {
		while (isNumeric(char)) consume()
		if (char == '.' && isNumeric(next)) {
			consume()
			while (isNumeric(char)) consume()
		}
		return createWithLexeme(NUMBER)
	}

	private fun string(): Token {
		start = current
		startOnLine = currentOnLine
		while (char != '"' && !atEnd()) {
			if (char == '\n') {
				line++
				return error(CompilerError.unterminatedString(char, currentOnLine, line))
			}
			consume()
		}
		if (atEnd()) error(CompilerError.unterminatedString(char, currentOnLine, line))
		val string = createWithLexeme(STRING)
		consume()
		return string
	}

	private fun keyword(): Token {
		while (isAlphaNumeric(char)) consume()
		val lexeme = source.subSequence(start, current)
		val type = when (lexeme) {
			"_" -> UNDERSCORE
			"fn" -> FN
			"as" -> AS
			"in" -> IN
			"if" -> IF
			"for" -> FOR
			"let" -> LET
			"mut" -> MUT
			"mod" -> MOD
			"else" -> ELSE
			"loop" -> LOOP
			"impl" -> IMPL
			"self" -> SELF
			"enum" -> ENUM
			"true" -> TRUE
			"false" -> FALSE
			"where" -> WHERE
			"while" -> WHILE
			"match" -> MATCH
			"break" -> BREAK
			"const" -> CONST
			"return" -> RETURN
			"struct" -> STRUCT
			"interface" -> INTERFACE
			"typealias" -> TYPEALIAS
			else -> IDENTIFIER
		}
		return if (type == IDENTIFIER) createWithLexeme(type)
		else create(type)
	}


	private fun skipWhiteSpace() {
		while (!atEnd()) {
			when (char) {
				' ', '\r' -> consume()
				'\t' -> {
					consume()
					currentOnLine += Zinc.INDENT_SIZE - 1
				}

				'\n' -> {
					line++
					consume()
					startOnLine = 0
					currentOnLine = 0
				}

				else -> break
			}
		}

		start = current
		startOnLine = currentOnLine
	}

	private fun create(type: TokenType): Token {
		start = current
		val previousRange = startOnLine..currentOnLine
		startOnLine = currentOnLine
		return Token(type, null, line, previousRange)
	}

	private fun createWithLexeme(type: TokenType): Token {
		val lexeme = source.subSequence(start, current)
		start = current
		val previousRange = startOnLine..currentOnLine
		startOnLine = currentOnLine
		return Token(type, lexeme, line, previousRange)
	}

	private fun error(error: CompilerError): Token {
		val lexeme = source.subSequence(start, current)
		start = current
		val previousRange = startOnLine..currentOnLine
		startOnLine = currentOnLine
		zinc.reportCompileError(error)
		return Token(ERROR, lexeme, line, previousRange)
	}

	private fun atEnd() = current >= source.length
	private fun atBeginning() = current <= 0
	private fun consume(): Char {
		currentOnLine++
		return source[current++]
	}

	private fun match(expected: Char): Boolean {
		if (atEnd() || source[current] != expected) return false
		consume()
		return true
	}

	private fun isAlpha(char: Char) = char in 'a'..'z' || char in 'A'..'Z' || char == '_'
	private fun isNumeric(char: Char) = char in '0'..'9'
	private fun isAlphaNumeric(char: Char) = isNumeric(char) || isAlpha(char)
}