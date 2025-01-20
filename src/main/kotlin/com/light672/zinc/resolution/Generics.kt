package com.light672.zinc.resolution

import com.light672.zinc.ast.GenericArgs as ASTGenericArgs
import com.light672.zinc.ast.GenericParams as ASTGenericParams

internal data class GenericArgs(val types: List<Type>, val ast: ASTGenericArgs)
internal class GenericParams(val parameters: List<TypeItem.Generic>, val ast: ASTGenericParams)
internal class WhereClause(val rules: List<Pair<Type, TypeBounds>>)
internal class TypeBounds(val traits: List<TypeItemReference>)