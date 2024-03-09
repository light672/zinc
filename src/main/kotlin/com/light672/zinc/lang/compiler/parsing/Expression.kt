package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.builtin.ZincValue

sealed class Expression {

	class InitializeStruct(val name: Token, val fields: Array<Pair<Token, Expression>>, val end: Token) : Expression() {
		override fun getRange() = name.range.first..end.range.last
	}

	class Unary(val operator: Token, val right: Expression) : Expression() {
		override fun getRange() = operator.range.first..right.getRange().last
	}

	class Binary(val left: Expression, val right: Expression, val operator: Token) : Expression() {
		override fun getRange() = left.getRange().first..right.getRange().last
	}

	class Literal(val value: ZincValue, val token: Token) : Expression() {
		override fun getRange() = token.range
	}

	class Grouping(val expression: Expression, val beginning: Token, val end: Token) : Expression() {
		override fun getRange() = beginning.range.first..end.range.last
	}

	class GetVariable(val variable: Token) : Expression() {
		override fun getRange() = variable.range
	}

	class SetVariable(val variable: Token, val value: Expression) : Expression() {
		override fun getRange() = variable.range.first..value.getRange().last
	}

	class GetField(val obj: Expression, val field: Token) : Expression() {
		override fun getRange() = obj.getRange().first..field.range.last
	}

	class SetField(val obj: Expression, val field: Token, val value: Expression) : Expression() {
		override fun getRange() = obj.getRange().first..value.getRange().last
	}

	class Call(val callee: Expression, val leftParen: Token, val arguments: Array<Expression>, val rightParen: Token) : Expression() {
		override fun getRange() = callee.getRange().first..rightParen.range.last
	}

	class Unit(val beginning: Token, val end: Token) : Expression() {
		override fun getRange() = beginning.range.first..end.range.last
	}

	class Return(val token: Token, val expression: Expression?) : Expression() {
		override fun getRange() = token.range.first..(expression?.getRange()?.last ?: token.range.last)
	}


	abstract fun getRange(): IntRange
}
