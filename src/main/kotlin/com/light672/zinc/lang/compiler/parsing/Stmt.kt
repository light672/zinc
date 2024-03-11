package com.light672.zinc.lang.compiler.parsing

internal sealed class Stmt {
	class ExpressionStatement(val expression: Expr, val sc: Token?) : Stmt() {
		override fun getRange() = expression.getRange().first..(sc?.range?.last ?: expression.getRange().last)
	}

	class Function(
		val declaration: Token,
		val name: Token,
		val arguments: Array<Pair<Token, Token>>,
		val rightParen: Token,
		val type: Token?,
		val body: Array<Stmt>,
		val closeToken: Token,
	) : Stmt() {
		override fun getRange() = declaration.range.first..(type?.range?.last ?: rightParen.range.last)
	}

	class Struct(
		val declaration: Token,
		val name: Token,
		val fields: Array<Pair<Token, Token>>,
		val closeToken: Token
	) : Stmt() {
		override fun getRange() = declaration.range.first..closeToken.range.last
	}

	class VariableDeclaration(val declaration: Token, val name: Token, val type: Token?, val initializer: Expr?, val sc: Token) : Stmt() {
		override fun getRange() = declaration.range.first..sc.range.last
	}

	abstract fun getRange(): IntRange
}