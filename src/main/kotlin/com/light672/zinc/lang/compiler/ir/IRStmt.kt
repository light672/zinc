package com.light672.zinc.lang.compiler.ir

internal sealed class IRStmt {
	class Expr(val expr: IRExpr, trailing: Boolean) : IRStmt()
	class LetBinding(val branch: Namespace.Branch, val pattern: Pattern) : IRStmt()
}