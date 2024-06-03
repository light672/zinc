package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.item.names.Struct

internal sealed class Type {
	data class Primitive(val struct: Struct) : Type()
	data class InitializedStruct(val struct: Struct) : Type()
	data class Tuple(val types: ArrayList<Type>) : Type()
	data class Function(val parameters: ArrayList<Type>, val returnType: Type) : Type()
	data class Trait(val trait: com.light672.zinc.lang.compiler.item.names.Trait) : Type()
	data object Generic : Type()
}