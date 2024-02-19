package zinc.lang.compiler


internal class ScopeHandler(val resolver: Resolver) {
	private val locals = ArrayList<Declaration>()
	private var globals = HashMap<String, Declaration>()

	private var depth = 0

	private sealed class Declaration(val depth: Int, val type: Type) {
		abstract val declaration: Statement
		abstract val name: String
		abstract val mutable: Boolean
		abstract var initializerRange: IntRange?
		val initialized get() = initializerRange != null

		class Variable(override val declaration: Statement.VariableDeclaration, type: Type, depth: Int) : Declaration(depth, type) {
			override val name = declaration.name.lexeme
			override val mutable = declaration.declaration.type == Token.Type.VAR
			override var initializerRange: IntRange? = null
		}

		class Function(override val declaration: Statement.Function, type: Type, depth: Int) : Declaration(depth, type) {
			override val name = declaration.name.lexeme
			override val mutable = false
			override var initializerRange: IntRange? = null
		}
	}

	fun defineFunction(declaration: Statement.Function, type: Type): Unit? {
		fun error(variable: Declaration) = CompilerError.TwoRangeError(
			variable.declaration.getRange(), declaration.getRange(),
			"Variable '${variable.name}' first declared here.",
			"Function '${declaration.name.lexeme}' declared again in the same scope.",
			"A variable with the name '${declaration.name.lexeme}' already exists in the current scope."
		)
		if (depth == 0) {
			val existingGlobal = globals[declaration.name.lexeme]
			if (existingGlobal != null) {
				resolver.instance.reportCompileError(error(existingGlobal))
				return null
			}
			val global = Declaration.Function(declaration, type, depth)
			global.initializerRange = declaration.getRange()
			globals[declaration.name.lexeme] = global
			return Unit
		}

		for (i in locals.size - 1 downTo 0) {
			val variable = locals[i]
			if (variable.depth < depth) break
			if (variable.name == declaration.name.lexeme) {
				resolver.instance.reportCompileError(error(variable))
				return null
			}
		}
		val local = Declaration.Function(declaration, type, depth)
		local.initializerRange = declaration.getRange()
		locals.add(local)
		return Unit
	}


	fun declareVariable(declaration: Statement.VariableDeclaration, type: Type): Unit? {
		fun error(variable: Declaration) = CompilerError.TwoRangeError(
			variable.declaration.getRange(), declaration.getRange(),
			"Variable '${variable.name}' first declared here.",
			"Variable '${declaration.name.lexeme}' declared again in the same scope.",
			"A variable with the name '${declaration.name.lexeme}' already exists in the current scope."
		)

		if (depth == 0) {
			val existingGlobal = globals[declaration.name.lexeme]
			if (existingGlobal != null) {
				resolver.instance.reportCompileError(error(existingGlobal))
				return null
			}
			val global = Declaration.Variable(declaration, type, depth)
			global.initializerRange = declaration.initializer?.getRange()
			globals[declaration.name.lexeme] = global
			return Unit
		}

		for (i in locals.size - 1 downTo 0) {
			val variable = locals[i]
			if (variable.depth < depth) break
			if (variable.name == declaration.name.lexeme) {
				resolver.instance.reportCompileError(error(variable))
				return null
			}
		}
		val local = Declaration.Variable(declaration, type, depth)
		local.initializerRange = declaration.initializer?.getRange()
		locals.add(local)
		return Unit
	}

	private fun setVariable(setExpr: Expression.SetVariable): Unit? {
		val expressionType = resolver.typeChecker.getType(setExpr)
		for (i in locals.size - 1 downTo 0) {
			val variable = locals[i]
			if (variable.name != setExpr.variable.lexeme) continue
			return setVariable(setExpr, variable, expressionType)
		}
		val global = globals[setExpr.variable.lexeme]
		if (global != null) return setVariable(setExpr, global, expressionType)
		resolver.instance.reportCompileError(
			CompilerError.OneRangeError(setExpr.getRange(), "Variable '${setExpr.variable.lexeme}' does not exist in the current scope.")
		)
		return null
	}

	private fun setVariable(setExpr: Expression.SetVariable, declaration: Declaration, expressionType: Type): Unit? {
		if (declaration.type != expressionType) {
			resolver.instance.reportCompileError(
				CompilerError.TwoRangeError(
					declaration.declaration.getRange(), setExpr.getRange(),
					"Variable declared with type '${declaration.type}'.",
					"Expression returns type '${expressionType}'.",
					"Declared type '${declaration.type}' does not match set type '${expressionType}'."
				)
			)
			return null
		}
		if (declaration.initialized && !declaration.mutable) {
			resolver.instance.reportCompileError(
				CompilerError.TwoRangeError(
					declaration.declaration.getRange(), setExpr.getRange(),
					"Variable first initialized here.",
					"Variable reassigned here.",
					"Immutable variable '${declaration.name}' reassigned after initialization."
				)
			)
			return null
		}
		if (!declaration.initialized) declaration.initializerRange = setExpr.getRange()
		return Unit
	}

	private fun getVariable(gotVariable: Token): Declaration? {
		for (i in locals.size - 1 downTo 0) {
			val variable = locals[i]
			if (variable !is Declaration.Variable || variable.name != gotVariable.lexeme) continue
			return getVariable(gotVariable, variable)
		}
		val global = globals[gotVariable.lexeme]
		if (global != null) return getVariable(gotVariable, global)
		resolver.instance.reportCompileError(
			CompilerError.OneRangeError(gotVariable.range, "Variable '${gotVariable.lexeme}' does not exist in the current scope.")
		)
		return null
	}

	private fun getVariable(gotVariable: Token, variable: Declaration): Declaration? {
		if (variable.initialized) return variable
		resolver.instance.reportCompileError(
			CompilerError.TwoRangeError(
				variable.declaration.getRange(), gotVariable.range,
				"Variable declared here but left uninitialized.",
				"${variable.name} used here but is not initialized.",
				"Variable ${variable.name} used before it has been initialized."
			)
		)
		return null
	}

	fun getVariableType(variable: Token) = getVariable(variable)?.type


	fun scope(block: () -> Unit) {
		val prev = locals.size
		depth++
		block()
		depth--
		while (locals.last().depth > depth) locals.removeAt(locals.lastIndex)
	}


}