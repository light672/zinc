package com.light672.zinc.lang.compiler

import com.light672.zinc.builtin.ZincBoolean
import com.light672.zinc.builtin.ZincChar
import com.light672.zinc.builtin.ZincNumber
import com.light672.zinc.builtin.ZincString
import com.light672.zinc.lang.compiler.core.Core
import com.light672.zinc.lang.compiler.parsing.syntax.Expr
import com.light672.zinc.lang.compiler.parsing.syntax.Stmt
import com.light672.zinc.lang.compiler.parsing.syntax.Token

internal class Resolver(val statements: ArrayList<Stmt>) {

	private fun Stmt.Expression.resolve(): Type {
		return expr.type()
	}

	private fun Expr.Literal.type(): Type {
		return when (literal) {
			is ZincNumber -> Core.numType
			is ZincChar -> Core.charType
			is ZincString -> Core.strType
			is ZincBoolean -> Core.boolType
			else -> throw IllegalArgumentException("not possible")
		}
	}

	private fun Expr.Binary.type(): Type {
		TODO()
		when (operator.type) {
			Token.Type.PLUS -> {

			}

			else -> throw IllegalArgumentException("not possible")
		}
	}

	private fun Expr.Unary.type(): Type {
		TODO()
	}

	private fun Expr.Variable.type(): Type {
		val variable = scope.getVariable(variable.lexeme)
		return variable.type
	}

	// <editor-fold desc="resolver stuff">
	private fun Stmt.resolve() {
		when (this) {
			is Stmt.Expression -> resolve()
			is Stmt.Variable -> resolve()
			is Stmt.Function -> resolve()
			is Stmt.Semicolon -> {}
		}
	}

	private fun Expr.type(): Type {
		return when (this) {
			is Expr.Binary -> type()
			is Expr.Unary -> type()
			is Expr.Group -> expr.type()
			is Expr.Literal -> type()
			is Expr.Variable -> type()
			is Expr.Block -> TODO()
		}
	}

	fun resolve() {
		for (stmt in statements) {
			stmt.resolve()
		}
	}

	fun scope(body: () -> Unit) {
		val prev = scope
		scope = Scope(HashMap(), HashMap(), HashMap(), prev.depth, prev)
		try {
			body()
		} catch (e: ResolverError) {
			scope = prev
			throw e
		}
		scope = prev
	}

	private var scope = Core.namespace.clone()

	class ResolverError(message: String) : RuntimeException(message)

	// </editor-fold>
}