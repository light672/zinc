package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.lang.compiler.ast.syntax.Stmt
import com.light672.zinc.lang.tool.Either

internal class Function(
	name: String,
	genericArity: Int,
	val declaration: Stmt.Function,
	val values: Namespace.Branch?,
	val types: Namespace.Branch?
) : Item(name, genericArity) {
	lateinit var parameters: List<Type>
	lateinit var returnType: Type
	lateinit var block: Either<Unit, IRExpr.Block>
}