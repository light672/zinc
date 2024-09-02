package com.light672.zinc.lang.compiler.ast.syntax

internal class TupleType(val open: Token, val fields: List<Type>, val close: Token) : Type {
	override fun range(): Token.Range {
		return open..close
	}
}