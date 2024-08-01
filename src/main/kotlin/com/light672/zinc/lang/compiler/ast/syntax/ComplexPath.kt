package com.light672.zinc.lang.compiler.ast.syntax

internal class ComplexPath(val tail: List<Segment>, val head: Segment) : Type {
	override fun range() = (tail.lastOrNull()?.segment ?: head.segment)..(head.generics?.close ?: head.segment)


	internal class Segment(val segment: Token, val generics: GenericArgs?) {
		companion object {
			fun none(doubleColon: Token) = Segment(Token.newNA("root", doubleColon.line, doubleColon.range), null)
		}
	}
}