package zinc.lang.compiler

open class Statement {
	class ExpressionStatement(val expression: Expression) : Statement()
	class VariableDeclaration(val variable: Variable, val initializer: Expression?) : Statement()
	class Function(val name: Token, val arguments: List<Variable>?, val body: List<Statement> = ArrayList(0)) :
		Statement()

	class Class(
		val name: Token,
		val constructorArgs: List<VariableDeclaration>?,
		val fields: List<VariableDeclaration>?,
		val methods: List<Function>?
	) : Statement()

	class For(val iterator: Token, val iterable: Expression, val body: List<Statement>) : Statement()
	class While(val condition: Expression, val body: List<Statement>) : Statement()
	class Return(val value: Expression?) : Statement()
	class Break() : Statement()
}