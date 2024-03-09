package com.light672.zinc.lang.compiler.parsing

sealed class Statement {
	class ExpressionStatement(val expression: Expression, val sc: Token?) : Statement() {
		override fun getRange() = expression.getRange().first..(sc?.range?.last ?: expression.getRange().last)
	}

	class Function(
		val declaration: Token,
		val name: Token,
		val arguments: Array<Pair<Token, Token>>,
		val rightParen: Token,
		val type: Token?,
		val body: Array<Statement>,
		val closeToken: Token,
	) : Statement() {
		override fun getRange() = declaration.range.first..(type?.range?.last ?: rightParen.range.last)
	}

	class Struct(
		val declaration: Token,
		val name: Token,
		val fields: Array<Pair<Token, Token>>,
		val closeToken: Token
	) : Statement() {
		override fun getRange() = declaration.range.first..closeToken.range.last
	}

	class VariableDeclaration(val declaration: Token, val name: Token, val type: Token?, val initializer: Expression?, val sc: Token) : Statement() {
		override fun getRange() = declaration.range.first..sc.range.last
	}

	abstract fun getRange(): IntRange
}