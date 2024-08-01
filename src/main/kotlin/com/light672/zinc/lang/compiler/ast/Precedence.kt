package com.light672.zinc.lang.compiler.ast

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
	UNARY, // prefix
	CALL // infix
}