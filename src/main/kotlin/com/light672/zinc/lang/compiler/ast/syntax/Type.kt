package com.light672.zinc.lang.compiler.ast.syntax

internal sealed interface Type {
	fun range(): Token.Range
}