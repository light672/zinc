package zinc.lang.compiler

import org.apache.commons.lang3.StringEscapeUtils
import zinc.lang.compiler.Token.Type.*

class Lexer(val source: String) {

	private var start = 0
	private var current = 0
	private var line = 1

	private fun currentChar() = if (end()) '\u0000' else source[current]
	private fun nextChar() = if (current + 1 >= source.length) '\u0000' else source[current + 1]

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
		start = current
		if (end()) return Token(EOF, line + 1, "EOF")
		val c = consume()
		return when (c) {
			'(' -> add(LEFT_PAREN)
			')' -> add(RIGHT_PAREN)
			'{' -> add(LEFT_BRACE)
			'}' -> add(RIGHT_BRACE)
			'[' -> add(LEFT_BRACKET)
			']' -> add(RIGHT_BRACKET)
			',' -> add(COMMA)
			'.' -> add(DOT)
			'+' -> {
				if (match('+')) add(PLUS_PLUS)
				else add(if (match('=')) PLUS_EQUAL else PLUS)
			}

			'-' -> {
				if (match('-')) add(MINUS_MINUS)
				else add(if (match('=')) MINUS_EQUAL else MINUS)
			}

			'*' -> add(if (match('=')) STAR_EQUAL else STAR)
			'^' -> add(if (match('=')) CARET_EQUAL else CARET)
			'/' -> {
				if (match('/')) {
					while (currentChar() != '\n' && !end()) consume()
					scanToken()
				} else if (match('=')) add(SLASH_EQUAL) else add(SLASH)
			}

			'%' -> add(if (match('=')) PERCENT_EQUAL else PERCENT)
			':' -> add(COLON)
			';' -> add(SEMICOLON)
			'?' -> add(QUESTION)
			'!' -> if (match('=')) add(BANG_EQUAL) else add(BANG)
			'=' -> if (match('=')) add(EQUAL_EQUAL) else add(EQUAL)
			'>' -> if (match('=')) add(GREATER_EQUAL) else add(GREATER)
			'<' -> if (match('=')) add(LESS_EQUAL) else add(LESS)
			'"' -> {
				if (!match('"'))
					string()
				else if (nextChar() == '"') {
					consume()
					multiLineString()
				} else {
					string()
				}
			}

			'\'' -> char()

			else -> {
				if (c.isDigit()) return number()
				else if (c.isAlpha()) return identifier()
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
			when (source[start]) {
				'a' -> if (current - start > 1)
					when (source[start + 1]) {
						's' -> return AS
						'n' -> return check(2, "d", AND)
					}

				'b' -> return check("reak", BREAK)
				'c' -> return check("lass", CLASS)
				'e' -> if (current - start > 1 && source[start + 1] == 'l')
					when (source[start + 2]) {
						'i' -> return check(3, "f", ELIF)
						's' -> return check(3, "e", ELSE)
					}

				'i' -> if (current - start > 1)
					when (source[start + 1]) {
						's' -> return IS
						'f' -> return IF
						'n' -> return IN
					}

				'l' -> return check("oop", LOOP)
				'n' -> return check("il", NIL)
				'o' -> return check("r", OR)
				'r' -> return check("eturn", RETURN)
				'v' -> return check("ar", VAR)
				'w' -> return check("hile", WHILE)
				't' -> return check("rue", TRUE)
				'f' -> if (current - start > 1)
					when (source[start + 1]) {
						'a' -> return check(2, "lse", FALSE)
						'o' -> return check(2, "r", FOR)
						'u' -> return check(2, "nc", FUNC)
					}
			}
			return IDENTIFIER
		}

		while (currentChar().isAlphaNumeric()) consume()
		return add(identifierToken())
	}

	private fun number(): Token {
		while (currentChar().isDigit()) consume()
		if (currentChar() == '.' && nextChar().isDigit()) {
			consume()
			while (currentChar().isDigit()) consume()
		}
		return add(NUMBER_VALUE)
	}

	private fun string(): Token {
		start = current
		while (currentChar() != '"' && !end()) {
			if (currentChar() == '\n') {
				line++
				return errorToken("Unterminated string on line. For multi-line strings use '\"\"\"'.")
			}
			consume()
		}

		if (end()) return errorToken("Unterminated string.")
		val token = addFormatted(STRING_VALUE)
		consume()
		return token
	}

	private fun multiLineString(): Token {
		start = current
		while (!end()) {
			if (match('"') && match('"') && match('"')) return addFormatted(STRING_VALUE)
			if (currentChar() == '\n') line++
			consume()
		}
		return errorToken("Unterminated multi-line string.")
	}

	private fun char(): Token {
		start = current
		while (currentChar() != '\'' && !end()) {
			if (currentChar() == '\n') {
				line++
				return errorToken("Character literals may only be on a single line.")
			}
			consume()
		}

		if (end()) return errorToken("Unterminated char.")
		val token = addFormatted(CHAR_VALUE)
		consume()
		return token
	}


	private fun skipWhiteSpace() {
		while (true) {
			if (end()) return
			when (currentChar()) {
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
	private fun match(expected: Char): Boolean {
		if (end() || source[current] != expected) return false
		current++
		return true
	}

	private fun end() = current >= source.length

	private fun Char.isDigit() = this in '0'..'9'
	private fun Char.isAlpha() = this in 'a'..'z' || this in 'A'..'Z' || this == '_'
	private fun Char.isAlphaNumeric() = isDigit() || isAlpha()
}