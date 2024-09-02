package com.light672.zinc.lang.compiler.ast.syntax

internal sealed class Pattern {
	class IdentifierPattern(val mut: Token?, val name: Token) : Pattern()
	class PathPattern(val path: Expr.Path) : Pattern()

	fun range(): Token.Range {
		return when (this) {
			is IdentifierPattern -> (mut ?: name)..name
			is PathPattern -> path.range()
		}
	}
}