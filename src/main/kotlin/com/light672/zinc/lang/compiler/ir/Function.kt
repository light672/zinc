package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.lang.compiler.ast.syntax.Stmt
import com.light672.zinc.lang.compiler.type_checking.Type
import com.light672.zinc.lang.tool.Either

internal class Function(
	val name: String,
	val declaration: Stmt.Function,
	val values: Namespace.Branch?,
	val types: Namespace.Branch?
) : Item() {
	lateinit var genericParameters: List<Type>
	lateinit var parameters: List<Type>
	lateinit var returnType: Type
	lateinit var block: Either<Unit, IRExpr.Block>
}