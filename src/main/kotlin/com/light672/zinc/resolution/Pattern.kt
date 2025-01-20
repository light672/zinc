package com.light672.zinc.resolution

import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Pattern as ASTPattern

internal sealed interface Pattern {
	data class Variable(val variable: ValueItem.Variable, val ast: ASTPattern.Identifier) : Pattern
	data class WildCard(val token: Token) : Pattern
	data class Literal(val token: Token) : Pattern
	data class Tuple(val fields: List<Pattern>, val ast: ASTPattern.Tuple) : Pattern

	fun range() = when (this) {
		is Literal -> token.asRange()
		is Tuple -> ast.range()
		is Variable -> ast.range()
		is WildCard -> token.asRange()
	}
}