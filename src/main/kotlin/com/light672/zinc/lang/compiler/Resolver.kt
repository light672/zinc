package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.ZincBoolean
import com.light672.zinc.builtin.ZincChar
import com.light672.zinc.builtin.ZincNumber
import com.light672.zinc.builtin.ZincString
import com.light672.zinc.lang.compiler.CompilerError.Companion.badArgs
import com.light672.zinc.lang.compiler.CompilerError.Companion.badBinaryOperator
import com.light672.zinc.lang.compiler.CompilerError.Companion.badCall
import com.light672.zinc.lang.compiler.CompilerError.Companion.badDot
import com.light672.zinc.lang.compiler.CompilerError.Companion.badFieldSetType
import com.light672.zinc.lang.compiler.CompilerError.Companion.badLogicalOperator
import com.light672.zinc.lang.compiler.CompilerError.Companion.badSetType
import com.light672.zinc.lang.compiler.CompilerError.Companion.badThenAndElse
import com.light672.zinc.lang.compiler.CompilerError.Companion.badType
import com.light672.zinc.lang.compiler.CompilerError.Companion.badUnaryOperator
import com.light672.zinc.lang.compiler.CompilerError.Companion.immutableSet
import com.light672.zinc.lang.compiler.CompilerError.Companion.matchingFunctionParameter
import com.light672.zinc.lang.compiler.CompilerError.Companion.matchingGlobal
import com.light672.zinc.lang.compiler.CompilerError.Companion.matchingType
import com.light672.zinc.lang.compiler.CompilerError.Companion.missingFields
import com.light672.zinc.lang.compiler.CompilerError.Companion.noElseBranch
import com.light672.zinc.lang.compiler.CompilerError.Companion.noFieldCalled
import com.light672.zinc.lang.compiler.CompilerError.Companion.noGlobalInit
import com.light672.zinc.lang.compiler.CompilerError.Companion.noMain
import com.light672.zinc.lang.compiler.CompilerError.Companion.noStruct
import com.light672.zinc.lang.compiler.CompilerError.Companion.noType
import com.light672.zinc.lang.compiler.CompilerError.Companion.noVariable
import com.light672.zinc.lang.compiler.CompilerError.Companion.notMatchingDeclaredType
import com.light672.zinc.lang.compiler.CompilerError.Companion.notMatchingReturnType
import com.light672.zinc.lang.compiler.CompilerError.Companion.usedBeforeInit
import com.light672.zinc.lang.compiler.parsing.Expr
import com.light672.zinc.lang.compiler.parsing.Stmt
import com.light672.zinc.lang.compiler.parsing.Token

internal class Resolver(val runtime: Zinc.Runtime, val module: ZincModule, val main: Boolean) {

	private var scope = module.globals
	private val global get() = scope.parent == null

	fun resolve(): Unit? {
		var err = false
		val structTypes = Array(module.structs.size) { i ->
			val resolved = module.structs[i].resolve()
			if (resolved == null) err = true
			resolved
		}
		if (err) return null

		for ((i, struct) in structTypes.withIndex())
			if (struct!!.resolveStructInside(module.structs[i].fields) == null) err = true
		if (err) return null

		val funcDeclarations = Array(module.functions.size) { i ->
			val resolved = module.functions[i].declare()
			if (resolved == null) err = true
			resolved
		}
		if (err) return null

		for (variable in module.variables)
			if (variable.resolve() == null) err = true
		if (err) return null

		for (declaration in funcDeclarations) {
			declaration!!.resolveFunctionBlock()
		}
		if (main && scope.getLocalVar("main")?.first?.type !is Type.Function) return error(noMain)
		return Unit
	}

	private fun Stmt.Struct.resolve(): Type.Struct? {
		if (scope.hasLocalType(name.lexeme)) return error(matchingType(range, name.lexeme))
		val type = Type.Struct(name.lexeme, LinkedHashMap())
		scope.structs[name.lexeme] = Struct(type, this)
		scope.types[name.lexeme] = type
		return type
	}

	private fun Type.Struct.resolveStructInside(fieldsArray: Array<Pair<Token, Token>>): Unit? {
		var fieldTypeError = false
		val fieldTypes = Array(fieldsArray.size) { i ->
			val type = fieldsArray[i].second
			getTypeFromName(type) ?: run {
				fieldTypeError = true
				null
			}
		}

		if (fieldTypeError) return null
		for ((i, field) in fieldsArray.withIndex()) fields[field.first.lexeme] =
			Pair(field.first.range.first..field.second.range.last, fieldTypes[i]!!)
		return Unit
	}

	private fun Stmt.VariableDeclaration.resolve(): Unit? {
		val mutable = declaration.type == Token.Type.VAR
		if (global && initializer == null) return error(noGlobalInit(range, name.lexeme))

		val declaredType = type?.let { getTypeFromName(it) ?: return null }
		val initializerType = initializer?.let { it.resolve() ?: return null }

		if (declaredType != null && initializerType != null && declaredType != initializerType)
			return error(notMatchingDeclaredType(range, declaredType, initializerType))

		val existing = scope.variables[name.lexeme]?.first
		if (existing != null && global) return error(matchingGlobal(existing.range, range, name.lexeme))

		val type = declaredType ?: initializerType!!
		scope.addVariable(name.lexeme, type, mutable, this, initializer != null)
		return Unit
	}

	private fun Stmt.Function.declare(): Declaration? {
		val declaredType = if (type != null) getTypeFromName(type) else Type.Unit
		var paramTypeError = false
		val paramTypes = Array(arguments.size) { i ->
			val type = arguments[i].second
			getTypeFromName(type) ?: run {
				paramTypeError = true
				null
			}
		}
		if (paramTypeError) return null
		val params = Array(paramTypes.size) { i -> paramTypes[i]!! }
		val og = scope.variables[name.lexeme]?.first
		if (scope.parent == null && og != null) return error(matchingGlobal(og.range, range, name.lexeme))
		declaredType ?: return null
		return scope.addVariable(name.lexeme, Type.Function(params, declaredType), false, this, true)
	}

	private fun Declaration.resolveFunctionBlock(): Unit? {
		return scope((type as Type.Function).returnType) scope@{
			statement as Stmt.Function
			for ((index, paramType) in type.parameters.withIndex()) {
				val pair = statement.arguments[index]
				val name = pair.first.lexeme
				val declaration = scope.variables[name]?.first
				if (declaration != null) {
					error<Unit>(matchingFunctionParameter(range, declaration.name))
					return@scope null
				}
				scope.addVariable(name, paramType, false, statement, true, pair.first.range.first..pair.second.range.last)
			}
			val finalType = statement.body.resolveAlreadyInScope(type.returnType != Type.Unit) ?: return@scope null
			if (!(scope.type == finalType || scope.type == Type.Unit)) error(
				notMatchingReturnType(
					statement.body.range,
					scope.type!!,
					finalType
				)
			) else Unit
		}
	}

	private fun Stmt.Function.resolve() = declare()?.resolveFunctionBlock()

	private fun Stmt.While.resolve(): Unit? {
		val conditionType = condition.resolve() ?: return null
		if (conditionType != Type.Bool) return error(badType(condition, conditionType, Type.Bool))

		then.resolve(false) ?: return null

		return Unit
	}


	private fun Expr.Return.resolve(): Type? {
		val type = expression?.let { it.resolve() ?: return null } ?: Type.Unit
		return if (scope.type != type) error(notMatchingReturnType(range, scope.type!!, type)) else
			Type.Never
	}

	private fun Expr.Unary.resolve(): Type? {
		val type = right.resolve() ?: return null
		return when (operator.type) {
			Token.Type.BANG -> if (type == Type.Bool) Type.Bool else error(badUnaryOperator(this, type))
			Token.Type.MINUS -> if (type == Type.Number) Type.Number else error(badUnaryOperator(this, type))
			else -> null
		}
	}

	private fun Expr.Binary.resolve(): Type? {
		val leftType = left.resolve() ?: return null
		val rightType = right.resolve() ?: return null
		if (leftType == Type.Number && rightType == Type.Number) return when (operator.type) {
			Token.Type.GREATER_EQUAL, Token.Type.GREATER, Token.Type.LESS_EQUAL, Token.Type.LESS -> Type.Bool
			else -> Type.Number
		}
		return error(badBinaryOperator(this, leftType, rightType))
	}

	private fun Expr.Logical.resolve(): Type? {
		val leftType = left.resolve() ?: return null
		val rightType = right.resolve() ?: return null
		if (leftType == Type.Bool && rightType == Type.Bool) return Type.Bool
		return error(badLogicalOperator(this, leftType, rightType))
	}

	private fun Expr.GetVariable.resolve(): Type? {
		val (variable, index) = findVariable(variable) ?: return null
		if (!variable.initialized) return error(usedBeforeInit(this.variable))
		return variable.type
	}

	private fun Expr.SetVariable.resolve(): Type? {
		val (variable, index) = findVariable(variable) ?: return null
		if (!variable.mutable) return error(immutableSet(variable, this))
		val expressionType = value.resolve() ?: return null
		if (variable.type != expressionType) return error(badSetType(variable, this, expressionType))
		variable.initialized = true
		return expressionType
	}

	private fun Expr.GetField.resolve(): Type? {
		val type = obj.resolve() ?: return null
		if (type !is Type.Struct) return error(badDot(obj, type))
		val (fieldRange, fieldType) = type.fields[field.lexeme] ?: return error(noFieldCalled(type, field))
		return fieldType
	}

	private fun Expr.SetField.resolve(): Type? {
		val type = obj.resolve() ?: return null
		val setType = value.resolve() ?: return null
		if (type !is Type.Struct) return error(badDot(obj, type))
		val field = type.fields[field.lexeme] ?: return error(noFieldCalled(type, field))
		if (setType != field.second) return error(badFieldSetType(field, setType, this.field, value, type))
		return setType
	}

	private fun Expr.Call.resolve(): Type? {
		val calleeType = callee.resolve() ?: return null
		if (calleeType !is Type.Function) return error(badCall(callee, calleeType))
		val argTypes = Array(calleeType.parameters.size) { i -> arguments[i].resolve() ?: return null }
		if (!argTypes.contentEquals(calleeType.parameters)) return error(badArgs(this, calleeType, argTypes))
		return calleeType.returnType
	}

	private fun Expr.InitializeStruct.resolve(): Type? {
		val struct = findStruct(name) ?: return null
		val map = struct.type.fields.clone() as LinkedHashMap<String, Pair<IntRange, Expr>>
		for ((name, expression) in fields) {
			val field = struct.type.fields[name.lexeme] ?: return error(noFieldCalled(struct.type, name))
			val exprType = expression.resolve() ?: return null
			if (exprType != field.second) return error(badFieldSetType(field, exprType, name, expression, struct.type))
			map.remove(name.lexeme)
		}
		if (map.isNotEmpty()) return error(missingFields(range, map, struct))
		return struct.type
	}

	private fun Expr.Block.resolve(valueUsed: Boolean): Type? {
		var type: Type? = null
		scope {
			type = resolveAlreadyInScope(valueUsed)
			Unit
		}
		return type
	}

	private fun Expr.If.resolve(valueUsed: Boolean): Type? {
		val conditionType = condition.resolve() ?: return null
		val thenType = thenBranch.resolve() ?: return null
		val elseType = elseBranch?.let { it.resolve() ?: return null } ?: if (valueUsed) return error(noElseBranch(this)) else Type.Never

		if (conditionType != Type.Bool) return error(badType(condition, conditionType, Type.Bool))
		elseBranch ?: return Type.Unit

		if (thenType != elseType) return error(badThenAndElse(thenBranch, elseBranch, thenType, elseType))
		return thenType
	}

	private fun Expr.Block.resolveAlreadyInScope(valueUsed: Boolean): Type? {
		var err = false
		val structTypes = Array(block.first.size) { i ->
			val resolved = block.first[i].resolve()
			if (resolved == null) err = true
			resolved
		}
		if (err) return null

		for ((i, struct) in structTypes.withIndex())
			if (struct!!.resolveStructInside(block.first[i].fields) == null) err = true
		if (err) return null
		for (stmt in block.second) {
			if (block.second.last() === stmt) {
				if (stmt !is Stmt.ExpressionStatement) {
					stmt.resolve()
					return Type.Unit
				}
				return stmt.expression.resolve(valueUsed)
			} else stmt.resolve()
		}
		// block is empty
		return Type.Unit
	}


	private fun findVariable(name: Token): Pair<Declaration, Int>? {
		var scope: Scope? = scope
		while (scope != null) {
			if (scope.variables[name.lexeme] != null) return scope.variables[name.lexeme]
			scope = scope.parent
		}
		return error(noVariable(name))
	}

	private fun findStruct(name: Token): Struct? {
		var scope: Scope? = scope
		while (scope != null) {
			if (scope.structs[name.lexeme] != null) return scope.structs[name.lexeme]
			scope = scope.parent
		}
		return error(noStruct(name))
	}


	private fun getTypeFromName(name: Token): Type? {
		var scope: Scope? = scope
		while (scope != null) {
			if (scope.types[name.lexeme] != null) return scope.types[name.lexeme]
			scope = scope.parent
		}
		return error(noType(name))
	}


	private fun scope(functionType: Type? = scope.type, block: () -> Unit?): Unit? {
		scope = Scope(scope, functionType, 0)
		val value = block()
		scope = scope.parent!!
		return value
	}

	private fun scope(block: () -> Unit?): Unit? {
		scope = Scope(scope)
		val value = block()
		scope = scope.parent!!
		return value
	}

	private fun Stmt.resolve() =
		when (this) {
			is Stmt.Struct -> null
			is Stmt.Function -> null
			is Stmt.VariableDeclaration -> resolve()
			is Stmt.While -> resolve()
			is Stmt.ExpressionStatement -> expression.resolve(false)?.let { Unit }
		}

	private fun Expr.resolve(valueUsed: Boolean = true): Type? {
		return when (this) {
			is Expr.Literal -> {
				when (value) {
					is ZincNumber -> Type.Number
					is ZincBoolean -> Type.Bool
					is ZincChar -> Type.Char
					is ZincString -> Type.String
					else -> null
				}
			}

			is Expr.Unit -> Type.Unit
			is Expr.Grouping -> expression.resolve()
			is Expr.Return -> resolve()
			is Expr.Unary -> resolve()
			is Expr.Binary -> resolve()
			is Expr.GetVariable -> resolve()
			is Expr.SetVariable -> resolve()
			is Expr.GetField -> resolve()
			is Expr.SetField -> resolve()
			is Expr.Call -> resolve()
			is Expr.InitializeStruct -> resolve()
			is Expr.Logical -> resolve()
			is Expr.Block -> resolve(valueUsed)
			is Expr.If -> resolve(valueUsed)
		}
	}

	private fun <T> error(compilerError: CompilerError): T? {
		runtime.reportCompileError(compilerError)
		return null
	}
}