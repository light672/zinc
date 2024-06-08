package com.light672.zinc.lang.compiler.parsing.syntax

internal class PathExprSegment(val segment: Token, val genericArgs: GenericArgs?) {
	companion object {
		val NONE = PathExprSegment(Token.empty(), null)
	}
}