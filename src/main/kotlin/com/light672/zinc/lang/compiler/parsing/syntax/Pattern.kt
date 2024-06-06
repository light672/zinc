package com.light672.zinc.lang.compiler.parsing.syntax

internal sealed class Pattern {
	class IdentifierPattern(val mut: Token?, val name: Token) : Pattern()
}