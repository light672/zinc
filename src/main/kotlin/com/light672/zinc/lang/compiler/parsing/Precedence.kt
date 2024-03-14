package com.light672.zinc.lang.compiler.parsing

enum class Precedence {
	NONE,
	ASSIGNMENT,
	TERNARY, // prefix
	OR, // infix
	AND, // infix
	EQUALITY, // infix
	COMPARISON, // infix
	TERM, // infix
	FACTOR, // infix
	EXPONENT, // infix
	UNARY, // prefix
	INIT, // infix
	CALL // infix
}