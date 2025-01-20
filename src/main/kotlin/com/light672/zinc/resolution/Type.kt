package com.light672.zinc.resolution

import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Type as ASTType

internal sealed interface Type {
	data class Tuple(val fields: List<Type>, val ast: ASTType.Tuple) : Type
	data class Item(val item: TypeItemReference, val ast: ASTType.Path) : Type
	data class Error(val range: Token.Range) : Type
}
