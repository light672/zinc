package com.light672.zinc.lang.compiler.ir

internal sealed class IRPattern {
	class Identifier(val variable: Variable) : IRPattern()
}