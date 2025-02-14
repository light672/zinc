package com.light672.zinc.hir

import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Pattern as ASTPattern

internal sealed interface Pattern {
	data class Literal(val token: Token) : Pattern
	data class Tuple(val fields: List<Pattern>, val ast: ASTPattern.Tuple) : Pattern
	data class Variable(val variable: com.light672.zinc.dsr.Variable, val ast: ASTPattern.Identifier) : Pattern
	data class Wildcard(val token: Token) : Pattern
}