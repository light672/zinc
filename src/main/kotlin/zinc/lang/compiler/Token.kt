package zinc.lang.compiler

data class Token(val type: Type, val line: Int, val lexeme: String = "") {
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
		COLON, SEMICOLON, QUESTION,
		BANG, BANG_EQUAL,
		EQUAL, EQUAL_EQUAL,
		GREATER, GREATER_EQUAL,
		LESS, LESS_EQUAL,
		IDENTIFIER, STRING_VALUE, CHAR_VALUE, DOUBLE_VALUE, TRUE, FALSE, NIL,
		CLASS, FUNC, VAR,
		FOR, WHILE, LOOP, IF, ELSE, ELIF,
		RETURN, BREAK,
		AND, OR,
		IS, AS, IN,
		EOF,
		ERROR
	}

	override fun toString(): String {
		return "Token($type, $line, $lexeme)"
	}
}