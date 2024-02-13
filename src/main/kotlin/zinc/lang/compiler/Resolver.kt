package zinc.lang.compiler

import zinc.Zinc
import zinc.builtin.ZincBoolean
import zinc.builtin.ZincChar
import zinc.builtin.ZincNumber
import zinc.builtin.ZincString

internal class Resolver(val instance: Zinc.Runtime) {
	private val global = Zinc.defaultGlobalScope.copy()
	var currentScope = global
	var inFunction: Pair<String, Array<Type>>? = null
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
		val mutable = declaration.type == Token.Type.VAR
		val definedType = type?.lexeme?.let {
			currentScope.getTypeOrNull(it)?.apply {
				initializer ?: currentScope.declareVariable(type.lexeme, this, mutable)
			} ?: run {
				instance.reportCompileError("Type '$it' does not exist in the current scope.")
				return null
			}
		}

		initializer?.run {
			initializer.resolve() ?: return null
			val inferredType = initializer.type() ?: return null
			definedType?.run {
				if (inferredType !== definedType) {
					instance.reportCompileError("Type '${type!!.lexeme}' does not match with initialized type of $type.")
					return null
				}
			}
			currentScope.declareVariable(name.lexeme, inferredType, mutable)
			currentScope.defineVariable(name.lexeme)
		}
		return Unit
	}

	private fun Statement.Function.resolve(): Unit? {
		val array = Array(arguments.size) {
			val type = currentScope.getTypeOrNull(arguments[it].second.lexeme) ?: run {
				instance.reportCompileError("Type '${arguments[it].second.lexeme}' does not exist in the current scope.")
				return null
			}
			Pair(arguments[it].first.lexeme, type)
		}

		val returnType = type?.let {
			currentScope.getTypeOrNull(type.lexeme) ?: run {
				instance.reportCompileError("Type '${it.lexeme}' does not exist in the current scope.")
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
			instance.reportCompileError("Not all paths in function return a value.")
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
			instance.reportCompileError("Invalid operands for '${operator.lexeme}', '${leftType}' and '${rightType}'.")
			return null
		}
		return Unit
	}

	private fun Expression.GetVariable.resolve(): Unit? {
		val variableInScope = currentScope.getVariableOrNull(variable.lexeme) ?: run {
			instance.reportCompileError("Variable '${variable.lexeme}' does not exist in the current scope.")
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
		return when (this) {
			is Expression.Unit -> Type.Unit
			is Expression.Return -> Type.Nothing
			is Expression.Grouping -> expression.type()
			is Expression.Literal -> type()
			is Expression.Binary -> type()
			is Expression.GetVariable -> type()
		}
	}

	private fun Expression.Literal.type() =
		when (value) {
			is ZincNumber -> Type.Number
			is ZincBoolean -> Type.Bool
			is ZincChar -> Type.Char
			is ZincString -> Type.String
			else -> throw IllegalArgumentException("There should be no other types in an Expression.Literal")
		}

	private fun Expression.Binary.type(): Type {
		if (left.type() === Type.Number) return Type.Number
		throw IllegalArgumentException("This should have been handled already")
	}

	private fun Expression.GetVariable.type() = currentScope.getVariableOrNull(variable.lexeme)!!.type

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

	private fun scope(block: () -> Unit) {
		currentScope = Scope(currentScope)
		block()
		currentScope = currentScope.parent!!
	}

	private fun functionScope(func: Pair<String, Array<Type>>, block: () -> Unit) {
		val previousFunction = inFunction
		inFunction = func
		scope(block)
		inFunction = previousFunction
	}

}