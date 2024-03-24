package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.builtin.ZincValue

internal sealed class Expr() {

	data class InitializeStruct(val name: Token, val fields: Array<Pair<Token, Expr>>, val end: Token) : Expr() {
		override val range = name.range.first..end.range.last
		override fun toString(): String {
			val builder = StringBuilder("${name.lexeme}{")
			for ((name, expr) in fields) {
				builder.append("${name.lexeme}: $expr, ")
			}
			if (builder[builder.length - 2] == ',') {
				builder.delete(builder.length - 2, builder.length)
			}
			builder.append("}")
			return builder.toString()
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as InitializeStruct

			if (name != other.name) return false
			if (!fields.contentEquals(other.fields)) return false
			if (end != other.end) return false

			return true
		}

		override fun hashCode(): Int {
			var result = name.hashCode()
			result = 31 * result + fields.contentHashCode()
			result = 31 * result + end.hashCode()
			return result
		}
	}

	data class Unary(val operator: Token, val right: Expr) : Expr() {
		override val range = operator.range.first..right.range.last
		override fun toString() = "${operator.lexeme}$right"
	}

	data class Binary(var left: Expr, var right: Expr, val operator: Token) : Expr() {
		override val range = left.range.first..right.range.last
		override fun toString() = "($left ${operator.lexeme} $right)"

		init {
			left.owner = this
			right.owner = this
		}
	}

	data class Logical(var left: Expr, var right: Expr, val operator: Token) : Expr() {
		override val range = left.range.first..right.range.last
		override fun toString() = "($left ${operator.lexeme} $right)"

		init {
			left.owner = this
			right.owner = this
		}
	}

	data class Literal(val value: ZincValue, val token: Token) : Expr() {
		override val range = token.range
		override fun toString() = "$value"
	}

	data class Grouping(val expression: Expr, val beginning: Token, val end: Token) : Expr() {
		override val range = beginning.range.first..end.range.last
		override fun toString() = "($expression)"
	}

	data class GetVariable(val variable: Token) : Expr() {
		override val range = variable.range
		override fun toString() = variable.lexeme
	}

	data class SetVariable(val variable: Token, val value: Expr) : Expr() {
		override val range = variable.range.first..value.range.last
		override fun toString() = "${variable.lexeme} = $value"
	}

	data class GetField(val obj: Expr, val field: Token) : Expr() {
		override val range = obj.range.first..field.range.last
		override fun toString() = "$obj.${field.lexeme}"
	}

	data class SetField(val obj: Expr, val field: Token, val value: Expr) : Expr() {
		override val range = obj.range.first..value.range.last
		override fun toString() = "$obj.${field.lexeme} = $value"
	}

	data class Call(val callee: Expr, val leftParen: Token, val arguments: Array<Expr>, val rightParen: Token) : Expr() {
		override val range = callee.range.first..rightParen.range.last
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Call

			if (callee != other.callee) return false
			if (leftParen != other.leftParen) return false
			if (!arguments.contentEquals(other.arguments)) return false
			if (rightParen != other.rightParen) return false

			return true
		}

		override fun hashCode(): Int {
			var result = callee.hashCode()
			result = 31 * result + leftParen.hashCode()
			result = 31 * result + arguments.contentHashCode()
			result = 31 * result + rightParen.hashCode()
			return result
		}

		override fun toString(): String {
			val builder = StringBuilder("$callee(")
			for (expr in arguments) {
				builder.append("$expr, ")
			}
			if (builder[builder.length - 2] == ',') {
				builder.delete(builder.length - 2, builder.length)
			}
			builder.append(")")
			return builder.toString()
		}
	}

	data class Unit(val beginning: Token, val end: Token) : Expr() {
		override val range = beginning.range.first..end.range.last
		override fun toString() = "()"
	}

	data class Return(val token: Token, val expression: Expr?) : Expr() {
		override val range = token.range.first..(expression?.range?.last ?: token.range.last)
		override fun toString() = "return${expression?.let { " $it" } ?: ""}"
	}


	abstract val range: IntRange
	var owner: Expr? = null
}
