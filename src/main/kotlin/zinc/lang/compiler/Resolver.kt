package zinc.lang.compiler

import zinc.Zinc

internal class Resolver(val instance: Zinc.Runtime) {
	val typeChecker = TypeChecker(this)
	val scopeHandler = ScopeHandler(this)

	fun resolve(ast: List<Statement>) {
		for (statement in ast) {
			statement.resolve()
		}
	}

	private fun Statement.resolve() {
		when (this) {
			is Statement.ExpressionStatement -> expression.resolve()
			is Statement.VariableDeclaration -> resolve()
			is Statement.Function -> resolve()
		}
	}

	private fun Statement.VariableDeclaration.resolve(): Unit? {
		initializer?.resolve()
		val type = initializer?.type()
		return scopeHandler.declareVariable(this, type)
	}

	private fun Statement.Function.resolve(): Unit? {
		val array = Array(arguments.size) {
			val type = currentScope.getTypeOrNull(arguments[it].second.lexeme) ?: run {
				instance.reportCompileError(
					CompilerError.TokenError(
						arguments[it].second,
						"Type '${arguments[it].second.lexeme}' does not exist in the current scope."
					)
				)
				return null
			}
			Pair(arguments[it].first.lexeme, type)
		}

		val returnType = type?.let {
			currentScope.getTypeOrNull(type.lexeme) ?: run {
				instance.reportCompileError(
					CompilerError.TokenError(
						it,
						"Type '${it.lexeme}' does not exist in the current scope."
					)
				)
				return null
			}
		} ?: Type.Unit

		val func = currentScope.defineAndDeclareFunction(name.lexeme, array, returnType, instance) ?: return null
		var properlyReturns = false
		functionScope(func) {
			for (pair in array) {
				currentScope.declareVariable(pair.first, pair.second, false)
				currentScope.defineVariable(pair.first)
			}
			properlyReturns = allPathsReturn(body)
		}
		return if (properlyReturns || returnType === Type.Unit) Unit else run {
			instance.reportCompileError(
				CompilerError.TokenError(
					closeToken,
					"Not all paths in function return a value."
				)
			)
			null
		}
	}

	private fun Expression.resolve(): Unit? =
		when (this) {
			is Expression.Literal -> Unit
			is Expression.Unit -> Unit
			is Expression.Grouping -> expression.resolve()
			is Expression.Binary -> resolve()
			is Expression.GetVariable -> resolve()
			is Expression.Return -> resolve()
		}


	private fun Expression.Binary.resolve(): Unit? {
		left.resolve() ?: return null
		right.resolve() ?: return null
		val leftType = left.type()
		val rightType = right.type()

		if (leftType !== Type.Number || rightType !== Type.Number) {
			instance.reportCompileError(
				CompilerError.OneRangeError(
					getRange(),
					"Invalid operands for '${operator.lexeme}', '${leftType}' and '${rightType}'."
				)
			)
			return null
		}
		return Unit
	}

	private fun Expression.GetVariable.resolve(): Unit? {
		val variableInScope = currentScope.getVariableOrNull(variable.lexeme) ?: run {
			instance.reportCompileError(
				CompilerError.TokenError(
					variable,
					"Variable '${variable.lexeme}' does not exist in the current scope."
				)
			)
			return null
		}
		if (!variableInScope.initialized) {
			instance.reportCompileError("Cannot use variable '${variable.lexeme}' before it is initialized.")
			return null
		}
		return Unit
	}

	private fun Expression.Return.resolve(): Unit? {
		val returnType = expression?.type() ?: Type.Unit
		// safe to typecast inFunction here because it will never make it to the resolver if statement is outside function
		val function = currentScope.getFunctionInParent(inFunction!!)!!
		if (function !== returnType) {
			instance.reportCompileError("Type returned '$returnType' does not match function return type of '$function'.")
			return null
		}
		return Unit
	}

	private fun Expression.type(): Type {
		return typeChecker.getType(this)
	}

	private fun allPathsReturn(block: Array<Statement>): Boolean {
		for (statement in block) {
			statement.resolve()
			if (when (statement) {
					is Statement.ExpressionStatement -> statement.expression is Expression.Return
					is Statement.Function -> continue
					is Statement.VariableDeclaration -> continue
				}
			) return true
		}
		return false
	}

}