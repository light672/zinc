package com.light672.zinc.lang.compiler.ir

internal sealed class Pattern {
	class Identifier(val variable: Variable) : Pattern()
}