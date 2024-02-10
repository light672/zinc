package zinc.lang.compiler

import zinc.builtin.ZincFalse
import zinc.builtin.ZincTrue
import zinc.builtin.ZincValue
import zinc.lang.Chunk
import zinc.lang.compiler.Token.Type.*
import zinc.lang.runtime.*

class Compiler : Expression.Visitor, Statement.Visitor {
	val code = ArrayList<Byte>()
	val constants = ArrayList<ZincValue>()
	val lines = ArrayList<Int>()
	fun compile(ast: List<Statement>): Chunk {
		for (statement in ast) {
			statement.resolve()
		}
		code.add(OP_END)
		return Chunk(code.toTypedArray(), constants.toTypedArray(), lines.toTypedArray())
	}

	override fun visit(expression: Expression.Binary) {
		expression.left.resolve()
		expression.right.resolve()
		when (expression.operator.type) {
			PLUS -> code.add(OP_ADD)
			MINUS -> code.add(OP_SUB)
			SLASH -> code.add(OP_DIV)
			STAR -> code.add(OP_MUL)
			PERCENT -> code.add(OP_MOD)
			CARET -> code.add(OP_POW)
			else -> throw IllegalArgumentException()
		}
	}

	override fun visit(expression: Expression.Literal) {
		when (expression.value) {
			is ZincTrue -> code.add(OP_TRUE)
			is ZincFalse -> code.add(OP_FALSE)
			else -> {
				constants.add(expression.value)
				code.add(OP_CONST)
				code.add((constants.size - 1).toByte())
			}
		}
	}

	override fun visit(expression: Expression.Grouping) {
		expression.expression.resolve()
	}

	override fun visit(expression: Statement.ExpressionStatement) {
		expression.expression.resolve()
		code.add(OP_POP)
	}

	override fun visit(expression: Statement.Function) {
		TODO("Not yet implemented")
	}

	override fun visit(expression: Statement.VariableDeclaration) {
		TODO("Not yet implemented")
	}


	private fun Expression.resolve() = accept(this@Compiler)
	private fun Statement.resolve() = accept(this@Compiler)
}