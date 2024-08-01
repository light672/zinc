package com.light672.zinc.lang.compiler.ast.syntax

import com.light672.zinc.lang.compiler.ir.Variable

internal sealed class Pattern {
	class IdentifierPattern(val mut: Token?, val name: Token) : Pattern()
	class PathPattern(val path: Expr.Path) : Pattern()


	fun toVariables(): List<Variable> {
		return when (this) {
			is IdentifierPattern -> listOf(Variable(name.lexeme, mut != null))
			is PathPattern -> emptyList()
		}
	}

	fun range(): Token.Range {
		return when (this) {
			is IdentifierPattern -> (mut ?: name)..name
			is PathPattern -> path.range()
		}
	}
}