package zinc.lang.compiler

abstract class Statement {
	interface Visitor {
		fun visit(statement: ExpressionStatement)
		fun visit(statement: Function)
		fun visit(statement: VariableDeclaration)
	}

	class ExpressionStatement(val expression: Expression) : Statement() {
		override fun accept(visitor: Visitor) = visitor.visit(this)
	}

	class Function(
		val name: Token,
		val arguments: Array<Pair<Token, Token>>,
		val type: Token?,
		val body: Array<Statement>
	) :
		Statement() {
		override fun accept(visitor: Visitor) = visitor.visit(this)
	}

	class VariableDeclaration(val declaration: Token, val name: Token, val type: Token?, val initializer: Expression?) :
		Statement() {
		override fun accept(visitor: Visitor) = visitor.visit(this)
	}


	abstract fun accept(visitor: Visitor)
}