package zinc.lang.compiler

import zinc.builtin.ZincFalse
import zinc.builtin.ZincNumber
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
		val isNumber = expression.right is Expression.Literal && expression.right.value is ZincNumber
		when (expression.operator.type) {
			PLUS -> code.add(if (isNumber) OP_ADD_NUM else OP_ADD)
			MINUS -> code.add(if (isNumber) OP_SUB_NUM else OP_SUB)
			SLASH -> code.add(if (isNumber) OP_DIV_NUM else OP_DIV)
			STAR -> code.add(if (isNumber) OP_MUL_NUM else OP_MUL)
			PERCENT -> code.add(if (isNumber) OP_MOD_NUM else OP_MOD)
			CARET -> code.add(if (isNumber) OP_POW_NUM else OP_POW)
			else -> throw IllegalArgumentException()
		}
	}

	override fun visit(expression: Expression.Literal) {
		when (expression.value) {
			is ZincTrue -> code.add(OP_TRUE)
			is ZincFalse -> code.add(OP_FALSE)
			null -> code.add(OP_NULL)
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


	private fun Expression.resolve() = accept(this@Compiler)
	private fun Statement.resolve() = accept(this@Compiler)
}