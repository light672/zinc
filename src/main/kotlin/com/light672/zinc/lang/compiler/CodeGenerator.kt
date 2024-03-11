package com.light672.zinc.lang.compiler

import com.light672.zinc.builtin.ZincFalse
import com.light672.zinc.builtin.ZincTrue
import com.light672.zinc.builtin.ZincValue
import com.light672.zinc.lang.Chunk
import com.light672.zinc.lang.compiler.parsing.Expr
import com.light672.zinc.lang.compiler.parsing.Stmt
import com.light672.zinc.lang.compiler.parsing.Token.Type.*
import com.light672.zinc.lang.runtime.*

class CodeGenerator {
	private val code = ArrayList<Byte>()
	private val constants = ArrayList<ZincValue>()
	private val lines = ArrayList<Int>()
	fun compile(ast: List<Stmt>): Chunk {
		for (statement in ast) {
			statement.compile()
		}
		code.add(OP_END)
		return Chunk(code.toTypedArray(), constants.toTypedArray(), lines.toTypedArray())
	}

	private fun Stmt.compile() {
		when (this) {
			is Stmt.ExpressionStatement -> compile()
			is Stmt.Function -> compile()
			is Stmt.VariableDeclaration -> compile()
			is Stmt.Struct -> {}
		}
	}

	private fun Stmt.ExpressionStatement.compile() {}

	private fun Stmt.Function.compile() {}

	private fun Stmt.VariableDeclaration.compile() {}

	private fun Expr.compile() {
		when (this) {
			is Expr.Literal -> compile()
			is Expr.Binary -> compile()
			is Expr.Grouping -> expression.compile()
			is Expr.GetVariable -> compile()
			else -> TODO("Not yet implemented.")
		}
	}

	private fun Expr.Literal.compile() {
		when (value) {
			is ZincTrue -> code.add(OP_TRUE)
			is ZincFalse -> code.add(OP_FALSE)
			else -> {
				constants.add(value)
				code.add(OP_CONST)
				code.add((constants.size - 1).toByte())
			}
		}
	}

	private fun Expr.Binary.compile() {
		left.compile()
		right.compile()
		when (operator.type) {
			PLUS -> code.add(OP_ADD)
			MINUS -> code.add(OP_SUB)
			SLASH -> code.add(OP_DIV)
			STAR -> code.add(OP_MUL)
			PERCENT -> code.add(OP_MOD)
			CARET -> code.add(OP_POW)
			else -> throw IllegalArgumentException()
		}
	}

	private fun Expr.GetVariable.compile() {}
}