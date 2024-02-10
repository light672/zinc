package zinc.lang.compiler

abstract class Statement {
	interface Visitor {
		fun visit(expression: ExpressionStatement)
		fun visit(expression: Function)
		fun visit(expression: VariableDeclaration)
	}

	class ExpressionStatement(val expression: Expression) : Statement() {
		override fun accept(visitor: Visitor) = visitor.visit(this)
	}

	class Function(name: Token, arguments: Array<Pair<Token, Token>>, type: Token?, body: Array<Statement>) :
		Statement() {
		override fun accept(visitor: Visitor) = visitor.visit(this)
	}

	class VariableDeclaration(declaration: Token, name: Token, type: Token?, initializer: Expression?) : Statement() {
		override fun accept(visitor: Visitor) = visitor.visit(this)
	}


	abstract fun accept(visitor: Visitor)
}