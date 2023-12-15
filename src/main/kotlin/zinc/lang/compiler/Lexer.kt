package zinc.lang.compiler

import org.apache.commons.lang3.StringEscapeUtils
import zinc.lang.compiler.Token.Type.*

class Lexer(val source: String) {

	private var start = 0
	private var current = 0
	private var line = 1

	private val currentChar get() = if (end) '\u0000' else source[current]
	private val nextChar get() = if (current + 1 >= source.length) '\u0000' else source[current + 1]

	private val end get() = current >= source.length

	fun scanTokens(): List<Token> {
		val tokens = emptyList<Token>()
		do {
			val t = scanToken()
		} while (t.type == EOF)
		return tokens
	}


	fun scanToken(): Token {
		skipWhiteSpace()
		start = current
		if (end) Token(EOF, line + 1)
		return when (currentChar) {
			'(' -> add(LEFT_PAREN)
			')' -> add(RIGHT_PAREN)
			'{' -> add(LEFT_BRACE)
			'}' -> add(RIGHT_BRACE)
			'[' -> add(LEFT_BRACKET)
			']' -> add(RIGHT_BRACKET)
			',' -> add(COMMA)
			'.' -> add(DOT)
			'+' -> if (match('+')) add(PLUS_PLUS) else if (match('=')) add(PLUS_EQUAL) else add(PLUS)
			'-' -> if (match('-')) add(MINUS_MINUS) else if (match('=')) add(MINUS_EQUAL) else add(MINUS)
			'*' -> if (match('=')) add(STAR_EQUAL) else add(STAR)
			'/' -> {
				if (match('/')) while (currentChar != '\n' && !end) consume()
				if (match('=')) add(SLASH_EQUAL) else add(SLASH)
			}

			'%' -> if (match('=')) add(PERCENT_EQUAL) else add(PERCENT)
			':' -> add(COLON)
			';' -> add(SEMICOLON)
			'?' -> add(QUESTION)
			'!' -> if (match('=')) add(BANG_EQUAL) else add(BANG)
			'=' -> if (match('=')) add(EQUAL_EQUAL) else add(EQUAL)
			'>' -> if (match('=')) add(GREATER_EQUAL) else add(GREATER)
			'<' -> if (match('=')) add(LESS_EQUAL) else add(LESS)
			'"' -> if (!match('"')) string() else if (match('"')) multiLineString() else {
				back()
				string()
			}

			'\'' -> char()

			else -> {
				if (currentChar.isDigit()) return number()
				else if (currentChar.isAlpha()) return identifier()
				else return errorToken("Unexpected character.")
			}
		}
	}

	private fun identifier(): Token {
		fun check(start: Int, rest: String, type: Token.Type) =
			if (current - this.start == start + rest.length && rest == source.substring(
					this.start + start,
					current
				)
			) type else IDENTIFIER

		fun check(rest: String, type: Token.Type) = check(1, rest, type)

		fun identifierToken(): Token.Type {
			return when (source[start]) {
				'a' -> if (current - start > 1)
					when (source[start + 1]) {
						's' -> AS
						'n' -> check(2, "d", AND)
						else -> IDENTIFIER
					} else IDENTIFIER

				'b' -> check("reak", BREAK)
				'c' -> check("lass", CLASS)
				'e' -> if (current - start > 1 && source[start + 1] == 'l')
					when (source[start + 2]) {
						'i' -> check(3, "f", ELIF)
						's' -> check(3, "e", ELSE)
						else -> IDENTIFIER
					} else IDENTIFIER

				'i' -> if (current - start > 1)
					when (source[start + 1]) {
						's' -> IS
						'f' -> IF
						'n' -> IN
						else -> IDENTIFIER
					} else IDENTIFIER

				'l' -> check("oop", LOOP)
				'n' -> check("il", NIL)
				'o' -> check("r", OR)
				'r' -> check("eturn", RETURN)
				'v' -> check("ar", VAR)
				'w' -> check("hile", WHILE)
				't' -> check("rue", TRUE)
				'f' -> if (current - start > 1)
					when (source[start + 1]) {
						'a' -> check(2, "lse", FALSE)
						'o' -> check(2, "r", FOR)
						'u' -> check(2, "nc", FUNC)
						else -> IDENTIFIER
					} else IDENTIFIER

				else -> IDENTIFIER
			}
		}

		while (currentChar.isAlphaNumeric()) consume()
		return add(identifierToken())
	}

	private fun number(): Token {
		while (currentChar.isDigit()) consume()
		if (currentChar == '.' && nextChar.isDigit()) {
			consume()
			while (currentChar.isDigit()) consume()
		}
		return this.add(DOUBLE_VALUE)
	}

	private fun string(): Token {
		start = current
		while (currentChar != '"' && !end) {
			if (currentChar == '\n') {
				line++
				return errorToken("Unterminated string on line. For multi-line strings use '\"\"\"'.")
			}
			consume()
		}

		if (end) return errorToken("Unterminated string.")
		val token = addFormatted(STRING_VALUE)
		consume()
		return token
	}

	private fun multiLineString(): Token {
		start = current
		while (!end) {
			if (match('"') && match('"') && match('"')) return addFormatted(STRING_VALUE)
			if (currentChar == '\n') line++
			consume()
		}
		return errorToken("Unterminated multi-line string.")
	}

	private fun char(): Token {
		start = current
		while (currentChar != '\'' && !end) {
			if (currentChar == '\n') {
				line++
				return errorToken("Character literals may only be on a single line.")
			}
			consume()
		}

		if (end) return errorToken("Unterminated char.")
		val token = addFormatted(CHAR_VALUE)
		consume()
		return token
	}


	private fun skipWhiteSpace() {
		while (true) {
			if (end) return
			when (currentChar) {
				' ', '\r', '\t' -> consume()
				'\n' -> {
					consume()
					line++
				}

				else -> return
			}
		}
	}

	private fun addFormatted(type: Token.Type): Token {
		val lexeme = source.substring(start, current)
		val final = StringEscapeUtils.escapeJava(lexeme)
		return Token(type, line, final)
	}

	private fun add(type: Token.Type): Token {
		val lexeme = source.substring(start, current)
		start = current
		return Token(type, line, lexeme)
	}

	private fun errorToken(message: String): Token {
		start = current
		return Token(ERROR, line, message)
	}

	private fun consume() = source[current++]
	private fun back() = source[--current]
	private fun match(expected: Char): Boolean {
		if (end || source[current] != expected) return false
		current++
		return true
	}

	private fun Char.isDigit() = this in '0'..'9'
	private fun Char.isAlpha() = this in 'a'..'z' || this in 'A'..'Z' || this == '_'
	private fun Char.isAlphaNumeric() = isDigit() || isAlpha()
}