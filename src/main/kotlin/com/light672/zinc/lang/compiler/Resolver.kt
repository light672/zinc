package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.CompilerError.Companion.badArgs
import com.light672.zinc.lang.compiler.CompilerError.Companion.badCall
import com.light672.zinc.lang.compiler.CompilerError.Companion.badDot
import com.light672.zinc.lang.compiler.CompilerError.Companion.badFieldSetType
import com.light672.zinc.lang.compiler.CompilerError.Companion.matchingGlobal
import com.light672.zinc.lang.compiler.CompilerError.Companion.missingFields
import com.light672.zinc.lang.compiler.CompilerError.Companion.noFieldCalled
import com.light672.zinc.lang.compiler.CompilerError.Companion.noGlobalInit
import com.light672.zinc.lang.compiler.CompilerError.Companion.notMatchingDeclaredType
import com.light672.zinc.lang.compiler.parsing.Expr
import com.light672.zinc.lang.compiler.parsing.Stmt
import com.light672.zinc.lang.compiler.parsing.Token
import com.light672.zinc.lang.runtime.opcodes.*

internal class Resolver(val runtime: Zinc.Runtime, val module: ZincModule) {

	private var scope = module.globals
	private val global get() = scope.parent == null

	fun Stmt.Struct.resolve(): Type.Struct? {
		if (scope.hasLocalStruct(name.lexeme) || scope.hasLocalType(name.lexeme)) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					scope.structs[name.lexeme]!!.statement.getRange(), getRange(),
					"Type '${scope.structs[name.lexeme]!!.type.name}' first declared here.",
					"'${name.lexeme}' declared here again.",
					"Type '${name.lexeme}' declared with same name as other type in the same scope."
				)
			)
			return null
		}
		val type = Type.Struct(name.lexeme, HashMap())
		scope.structs[name.lexeme] = Struct(type, this)
		scope.types[name.lexeme] = type
		return type
	}

	fun Type.Struct.resolveStructInside(fieldsArray: Array<Pair<Token, Token>>): Unit? {
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

	fun Stmt.VariableDeclaration.resolve(): Unit? {
		val mutable = declaration.type == Token.Type.VAR
		if (global && initializer == null) return error(noGlobalInit(range, name.lexeme))

		val declaredType = type?.let { getTypeFromName(it) ?: return null }
		val initializerType = initializer?.let { it.resolve() ?: return null }

		if (declaredType != null && initializerType != null && declaredType != initializerType)
			return error(notMatchingDeclaredType(range, declaredType, initializerType))

		val existing = scope.variables[name.lexeme]?.first
		if (existing != null && global) return error(matchingGlobal(existing.statement.range, range, name.lexeme))

		val type = declaredType ?: initializerType!!
		scope.addVariable(name.lexeme, type, mutable, this, initializer?.range, false)
		return Unit
	}

	fun Declaration.resolveFunctionBlock() {
		scope((type as Type.Function).returnType) {
			statement as Stmt.Function
			for ((index, paramType) in type.parameters.withIndex()) {
				val pair = statement.arguments[index]
				val name = pair.first.lexeme
				val range = pair.first.range.first..pair.second.range.last
				val declaration = scope.variables[name]?.first
				if (declaration != null) {
					runtime.reportCompileError(
						CompilerError.TwoRangeError(
							declaration.initRange!!, range,
							"Function parameter '${declaration.name}' first declared here.",
							"'$name' declared here again in the same function.",
							"Function parameter '$name' declared twice in the function parameters."
						)
					)
					return
				}
				scope.addVariable(name, paramType, false, statement, range, false)
			}
			for (stmt in statement.body) stmt.resolve()
		}
	}

	fun Stmt.Function.resolve(): Declaration? {
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
		if (scope.parent == null && og != null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					og.statement.getRange(), getRange(),
					"'${name.lexeme}' first declared here in top level scope.",
					"'${name.lexeme}' declared again in top level scope.",
					"'${name.lexeme}' can only be declared once in top level scope."
				)
			)
			return null
		}

		declaredType ?: return null

		val declaration = scope.addVariable(name.lexeme, Type.Function(params, declaredType), false, this, getRange(), true)
		return declaration
	}


	fun Expr.Return.resolve(): Type? {
		val type = expression?.let { it.resolve() ?: return null } ?: Type.Unit
		if (scope.type != type) {
			runtime.reportCompileError(
				CompilerError.OneRangeError(
					getRange(),
					"Return type '$type' does not match function return type of '${scope.type}'."
				)
			)
			return null
		}
		return Type.Nothing
	}

	fun Expr.Unary.resolve(): Type? {
		val type = right.resolve() ?: return null
		return when (operator.type) {
			Token.Type.BANG -> {
				if (type == Type.Bool)
					Type.Bool
				else {
					runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '!' on $type"))
					null
				}
			}

			Token.Type.MINUS -> {
				if (type == Type.Number)
					Type.Number
				else {
					runtime.reportCompileError(CompilerError.OneRangeError(right.getRange(), "Cannot perform unary '-' on $type"))
					null
				}
			}

			else -> throw IllegalArgumentException("should not happen")
		}
	}

	fun Expr.Binary.resolve(): Type? {
		val leftType = left.resolve() ?: return null
		val rightType = right.resolve() ?: return null
		if (leftType == Type.Number && rightType == Type.Number) return Type.Number
		runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Cannot perform binary '${operator.lexeme}' on '$leftType' and '$rightType'."
			)
		)
		return null
	}

	fun Expr.Logical.resolve(): Type? {
		val leftType = left.resolve() ?: return null
		val rightType = right.resolve() ?: return null
		if (leftType == Type.Bool && rightType == Type.Bool) return Type.Bool
		runtime.reportCompileError(
			CompilerError.OneRangeError(
				getRange(),
				"Cannot perform logical '${operator.lexeme}' on '$leftType' and '$rightType'."
			)
		)
		return null
	}

	fun Expr.GetVariable.resolve(): Type? {
		val (variable, index) = findVariable(variable) ?: return null
		if (variable.initRange == null) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					variable.statement.getRange(), getRange(),
					"Variable declared here without an initializer.",
					"Variable used before being initialized.",
					"Variable '${variable.name}' used before being initialized."
				)
			)
			return null
		}
		return variable.type
	}

	fun Expr.SetVariable.resolve(): Type? {
		val (variable, index) = findVariable(variable) ?: return null
		val expressionType = value.resolve() ?: return null
		if (variable.type != expressionType) {
			runtime.reportCompileError(
				CompilerError.TwoRangeError(
					variable.statement.getRange(), getRange(),
					"Declared with type '${variable.type}'.",
					"Set with type '$expressionType'.",
					"Value being set to '${variable.name}' has type '$expressionType', while '${variable.name}' is type '${variable.type}'."
				)
			)
			return null
		}
		if (variable.initRange == null) variable.initRange = getRange()
		return expressionType
	}

	fun Expr.GetField.resolve(): Type? {
		val type = obj.resolve() ?: return null
		if (type !is Type.Struct) {
			runtime.reportCompileError(CompilerError.OneRangeError(obj.getRange(), "Cannot get field using '.' on type '$type'."))
			return null
		}
		if (!type.fields.containsKey(field.lexeme)) {
			runtime.reportCompileError(CompilerError.TokenError(field, "Field '${field.lexeme}' does not exist in struct '$type'."))
			return null
		}
		return type.fields[field.lexeme]!!.second
	}

	fun Expr.SetField.resolve(): Type? {
		val type = obj.resolve() ?: return null
		val setType = value.resolve() ?: return null
		if (type !is Type.Struct) return error(badDot(obj, type))
		val field = type.fields[field.lexeme] ?: return error(noFieldCalled(type, field))
		if (setType != field.second) return error(badFieldSetType(field, setType, this.field, value, type))
		return setType
	}

	fun Expr.Call.resolve(): Type? {
		val calleeType = callee.resolve() ?: return null
		if (calleeType !is Type.Function) return error(badCall(callee, calleeType))
		val argTypes = Array(calleeType.parameters.size) { i -> arguments[i].resolve() ?: return null }
		if (!argTypes.contentEquals(calleeType.parameters)) return error(badArgs(this, calleeType, argTypes))
		return calleeType.returnType
	}

	fun Expr.InitializeStruct.resolve(): Type? {
		val struct = findStruct(name) ?: return null
		val map = struct.type.fields.clone() as HashMap<String, Pair<IntRange, Expr>>
		for ((name, expression) in fields) {
			val field = struct.type.fields[name.lexeme] ?: return error(noFieldCalled(struct.type, name))
			val exprType = expression.resolve() ?: return null
			if (exprType != field.second) return error(badFieldSetType(field, exprType, name, expression, struct.type))
			map.remove(name.lexeme)
		}
		if (map.isNotEmpty()) return error(missingFields(range, map, struct))
		return struct.type
	}


	fun findVariable(name: Token): Pair<Declaration, Int>? {
		var scope: Scope? = scope
		while (scope != null) {
			if (scope.variables[name.lexeme] != null) return scope.variables[name.lexeme]
			scope = scope.parent
		}
		runtime.reportCompileError(CompilerError.TokenError(name, "Variable '${name.lexeme}' does not exist in the current scope."))
		return null
	}

	fun findStruct(name: Token): Struct? {
		var scope: Scope? = scope
		while (scope != null) {
			if (scope.structs[name.lexeme] != null) return scope.structs[name.lexeme]
			scope = scope.parent
		}
		runtime.reportCompileError(
			CompilerError.TokenError(
				name,
				"Struct or variant '${name.lexeme}' does not exist in the current scope."
			)
		)
		return null
	}


	fun getTypeFromName(name: Token): Type? {
		var scope: Scope? = scope
		while (scope != null) {
			if (scope.types[name.lexeme] != null) return scope.types[name.lexeme]
			scope = scope.parent
		}
		runtime.reportCompileError(CompilerError.TokenError(name, "Type '${name.lexeme}' does not exist in the current scope."))
		return null
	}


	private inline fun scope(functionType: Type? = scope.type, block: () -> Unit) {
		scope = Scope(scope, functionType, 0)
		block()
		scope = scope.parent!!
	}

	private inline fun scope(block: () -> Unit) {
		scope = Scope(scope)
		block()
		scope = scope.parent!!
	}

	fun Stmt.resolve() =
		when (this) {
			is Stmt.Struct -> null
			is Stmt.Function -> null
			is Stmt.VariableDeclaration -> resolve()
			is Stmt.ExpressionStatement -> expression.resolve()?.let { Unit }
		}

	fun Expr.resolve(): Type? {
		return when (this) {
			is Expr.Literal -> {
				when (value) {
					is ZincNumber -> Type.Number
					is ZincBoolean -> Type.Bool
					is ZincChar -> Type.Char
					is ZincString -> Type.String
					else -> throw IllegalArgumentException("how did you even mess this up")
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
		}
	}

	fun <T> error(compilerError: CompilerError): T? {
		runtime.reportCompileError(compilerError)
		return null
	}
}