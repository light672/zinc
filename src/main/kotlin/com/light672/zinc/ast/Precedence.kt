package com.light672.zinc.ast

internal enum class Precedence {
	NONE,
	ASSIGNMENT,
	OR,
	AND,
	EQUALITY,
	COMPARISON,
	BIT_OR,
	BIT_XOR,
	BIT_AND,
	BIT_SHIFT,
	TERM,
	FACTOR,
	UNARY,
	CALL
}