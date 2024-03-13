package com.light672.zinc.lang.compiler.parsing

enum class Precedence {
	NONE,
	ASSIGNMENT,
	TERNARY,
	OR,
	AND,
	EQUALITY,
	COMPARISON,
	TERM,
	FACTOR,
	EXPONENT,
	UNARY,
	INIT,
	CALL
}