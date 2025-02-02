package com.light672.zinc.dsr

import com.light672.zinc.ast.Pattern as ASTPattern

internal sealed interface Pattern {
	data class Variable(val variable: com.light672.zinc.dsr.Variable, val ast: ASTPattern.Identifier) : Pattern
	data class Wildcard(val ast: ASTPattern.Wildcard) : Pattern
	data class Literal(val ast: ASTPattern.Literal) : Pattern
	data class Tuple(val fields: List<Pattern>, val ast: ASTPattern.Tuple) : Pattern
}