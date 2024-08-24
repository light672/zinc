package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.lang.compiler.ast.syntax.Stmt

internal sealed class IRStmt {
	class Expr(val expr: IRExpr, trailing: Boolean) : IRStmt()
	class LetBinding(val ast: Stmt.Variable, val branch: Namespace.Branch, val pattern: IRPattern) : IRStmt() {
		var initializer: IRExpr? = null
		var type: Type? = null
	}
}