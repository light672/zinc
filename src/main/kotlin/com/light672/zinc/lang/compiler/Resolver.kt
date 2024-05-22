package com.light672.zinc.lang.compiler

import com.light672.zinc.builtin.ZincNumber

internal class Resolver(val statements: ArrayList<Stmt>) {

	private fun Stmt.Expression.resolve() {

	}

	private fun Expr.Literal.type() {
		when (literal) {
			is ZincNumber ->
		}
	}

	private fun Expr.Binary.type() {

	}

	private fun Expr.Unary.type() {

	}

	// <editor-fold desc="resolver stuff">
	private fun Stmt.resolve() {
		when (this) {
			is Stmt.Expression -> resolve()
		}
	}

	private fun Expr.type() {
		when (this) {
			is Expr.Binary -> type()
			is Expr.Unary -> type()
			is Expr.Group -> expr.type()
			is Expr.Literal -> type()
		}
	}

	fun resolve() {
		for (stmt in statements) {
			stmt.resolve()
		}
	}


	// </editor-fold>
}