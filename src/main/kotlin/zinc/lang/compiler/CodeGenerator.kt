package zinc.lang.compiler

import zinc.builtin.ZincFalse
import zinc.builtin.ZincTrue
import zinc.builtin.ZincValue
import zinc.lang.Chunk
import zinc.lang.compiler.Token.Type.*
import zinc.lang.runtime.*

class CodeGenerator {
	private val code = ArrayList<Byte>()
	private val constants = ArrayList<ZincValue>()
	private val lines = ArrayList<Int>()
	fun compile(ast: List<Statement>): Chunk {
		for (statement in ast) {
			statement.compile()
		}
		code.add(OP_END)
		return Chunk(code.toTypedArray(), constants.toTypedArray(), lines.toTypedArray())
	}

	private fun Statement.compile() {
		when (this) {
			is Statement.ExpressionStatement -> compile()
			is Statement.Function -> compile()
			is Statement.VariableDeclaration -> compile()
		}
	}

	private fun Statement.ExpressionStatement.compile() {}

	private fun Statement.Function.compile() {}

	private fun Statement.VariableDeclaration.compile() {}

	private fun Expression.compile() {
		when (this) {
			is Expression.Literal -> compile()
			is Expression.Binary -> compile()
			is Expression.Grouping -> expression.compile()
			is Expression.GetVariable -> compile()
			else -> TODO("Not yet implemented.")
		}
	}

	private fun Expression.Literal.compile() {
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

	private fun Expression.Binary.compile() {
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

	private fun Expression.GetVariable.compile() {}
}