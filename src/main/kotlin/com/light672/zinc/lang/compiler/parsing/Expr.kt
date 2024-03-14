package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.builtin.ZincValue

internal sealed class Expr {

	class InitializeStruct(val name: Token, val fields: Array<Pair<Token, Expr>>, val end: Token) : Expr() {
		override fun getRange() = name.range.first..end.range.last
	}

	class Unary(val operator: Token, val right: Expr) : Expr() {
		override fun getRange() = operator.range.first..right.getRange().last
	}

	class Binary(var left: Expr, var right: Expr, val operator: Token) : Expr() {
		override fun getRange() = left.getRange().first..right.getRange().last

		init {
			left.owner = this
			right.owner = this
		}
	}

	class Logical(val left: Expr, val right: Expr, val operator: Token) : Expr() {
		override fun getRange() = left.getRange().first..right.getRange().last
	}

	class Literal(val value: ZincValue, val token: Token) : Expr() {
		override fun getRange() = token.range
	}

	class Grouping(val expression: Expr, val beginning: Token, val end: Token) : Expr() {
		override fun getRange() = beginning.range.first..end.range.last
	}

	class GetVariable(val variable: Token) : Expr() {
		override fun getRange() = variable.range
	}

	class SetVariable(val variable: Token, val value: Expr) : Expr() {
		override fun getRange() = variable.range.first..value.getRange().last
	}

	class GetField(val obj: Expr, val field: Token) : Expr() {
		override fun getRange() = obj.getRange().first..field.range.last
	}

	class SetField(val obj: Expr, val field: Token, val value: Expr) : Expr() {
		override fun getRange() = obj.getRange().first..value.getRange().last
	}

	class Call(val callee: Expr, val leftParen: Token, val arguments: Array<Expr>, val rightParen: Token) : Expr() {
		override fun getRange() = callee.getRange().first..rightParen.range.last
	}

	class Unit(val beginning: Token, val end: Token) : Expr() {
		override fun getRange() = beginning.range.first..end.range.last
	}

	class Return(val token: Token, val expression: Expr?) : Expr() {
		override fun getRange() = token.range.first..(expression?.getRange()?.last ?: token.range.last)
	}


	abstract fun getRange(): IntRange
	var owner: Binary? = null
}
