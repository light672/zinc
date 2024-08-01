package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.builtin.ZincValue

internal sealed class IRExpr {
	class Variable(val variable: com.light672.zinc.lang.compiler.ir.Variable) : IRExpr() {
		companion object {
			val ERROR = Variable(com.light672.zinc.lang.compiler.ir.Variable.ERROR)
		}
	}

	class Literal(val literal: ZincValue) : IRExpr()
	class Binary(val left: IRExpr, val right: IRExpr, val operator: BinaryOp) : IRExpr() {
		enum class BinaryOp {
			ADD,
			SUBTRACT,
			DIVIDE,
			MULTIPLY,
			MODULO,
			GREATER,
			GREATER_EQUAL,
			LESS,
			LESS_EQUAL,
			EQUAL,
			BIT_OR,
			BIT_AND,
			BIT_XOR,
			BIT_L_SHIFT,
			BIT_R_SHIFT,
			BIT_R_SHIFT_UNSIGNED,
			OR,
			AND,
		}
	}

	class Unary(val expr: IRExpr, val operator: UnaryOp) : IRExpr() {
		enum class UnaryOp {
			NOT,
			NEGATE,
			INVERT
		}
	}

	class Block(val stmts: List<IRStmt>) : IRExpr()
}