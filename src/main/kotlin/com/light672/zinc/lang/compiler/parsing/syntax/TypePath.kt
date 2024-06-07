package com.light672.zinc.lang.compiler.parsing.syntax

internal class TypePath(val tail: List<TypePathSegment>, val head: TypePathSegment) : Type {
	// ::? TypePathSegment (:: TypePathSegment)*
	class TypePathSegment(val segment: Token, val generics: GenericArgs?) {
		// IDENTIFIER (::? GenericArgs)?
		companion object {
			val NONE = TypePathSegment(Token.empty(), null)
		}
	}
}