package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.lang.compiler.ast.syntax.Stmt
import com.light672.zinc.lang.compiler.type_checking.Type

internal sealed class IRStmt {
	class Expr(val expr: IRExpr, trailing: Boolean) : IRStmt()
	class LetBinding(val ast: Stmt.Variable, val branch: Namespace.Branch, val pattern: IRPattern) : IRStmt() {
		var initializer: IRExpr? = null
		lateinit var type: Type
	}
}