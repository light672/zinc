package zinc.lang.compiler

sealed class Statement {
	class ExpressionStatement(val expression: Expression) : Statement()

	class Function(
		val name: Token,
		val arguments: Array<Pair<Token, Token>>,
		val type: Token?,
		val body: Array<Statement>
	) : Statement()

	class VariableDeclaration(val declaration: Token, val name: Token, val type: Token?, val initializer: Expression?) :
		Statement()
}