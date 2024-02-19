package zinc.lang.compiler

sealed class Statement {
	class ExpressionStatement(val expression: Expression) : Statement() {
		override fun getRange() = expression.getRange()
	}

	class Function(
		val declaration: Token,
		val name: Token,
		val arguments: Array<Pair<Token, Token>>,
		val rightParen: Token,
		val type: Token?,
		val body: Array<Statement>,
		val closeToken: Token
	) : Statement() {
		override fun getRange() = declaration.range.first..(type?.range?.last ?: rightParen.range.last)
	}

	class VariableDeclaration(val declaration: Token, val name: Token, val type: Token?, val initializer: Expression?) : Statement() {
		override fun getRange() = declaration.range.first..(initializer?.getRange()?.last ?: type!!.range.last)
	}

	abstract fun getRange(): IntRange
}