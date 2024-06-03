package com.light672.zinc.lang.compiler.parsing.syntax

internal class TypePath(val tail: TypePathSegment, val body: List<TypePathSegment>) : Type {
	// ::? TypePathSegment (:: TypePathSegment)*
	class TypePathSegment(val segment: Token, val generics: GenericArgs?) {
		// IDENTIFIER (::? GenericArgs)?
	}
}