package zinc.lang.compiler

data class Token(val type: Type, val line: Int, val lexeme: String = "") {
	companion object {
		fun empty(): Token {
			return Token(Type.NA, 0)
		}
	}

	enum class Type {
		LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACKET, RIGHT_BRACKET,
		COMMA, DOT,
		PLUS, PLUS_EQUAL,
		PLUS_PLUS,
		MINUS, MINUS_EQUAL,
		MINUS_MINUS,
		STAR, STAR_EQUAL,
		SLASH, SLASH_EQUAL,
		PERCENT, PERCENT_EQUAL,
		CARET, CARET_EQUAL,
		COLON, SEMICOLON, QUESTION,
		BANG, BANG_EQUAL,
		EQUAL, EQUAL_EQUAL,
		GREATER, GREATER_EQUAL,
		LESS, LESS_EQUAL,
		IDENTIFIER, STRING_VALUE, CHAR_VALUE, NUMBER_VALUE, TRUE, FALSE,
		STRUCT, FUNC, VAR, VAL,
		PUB, INT,
		FOR, WHILE, LOOP, IF, ELSE, ELIF,
		RETURN, BREAK,
		AND, OR,
		IS, AS, IN,
		EOF,
		ERROR,
		NA
	}

	override fun toString(): String {
		return "Token($type, $line, $lexeme)"
	}
}