package com.light672.zinc.dsr

import com.light672.zinc.ast.GenericParams as ASTGenericParams

internal class GenericParams(
	val params: List<Generic>,
	val branch: Branch<TypeItem>,
	val ast: ASTGenericParams
)