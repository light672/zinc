package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.*
import com.light672.zinc.lang.compiler.Token.Type.*
import com.light672.zinc.lang.runtime.opcodes.*
import java.lang.Double.parseDouble

internal class Compiler(source: String, val runtime: Zinc.Runtime) {
	private var scopeDepth = 0
	private val scopes = arrayOfNulls<Scope>(256)
	private val scope get() = scopes[scopeDepth]!!
	fun compile() {
		advance()
		while (!end()) declaration() ?: synchronize()
	}

	private fun declaration(): Unit? {
		if (match(STRUCT)) return structDeclaration()
		if (match(DEF)) return functionDeclaration()
		if (match(arrayOf(VAR, VAL))) return variableDeclaration()
		if (match(IMPL)) return implDeclaration()
		return statement()
	}

	private fun structDeclaration() {

	}

	private fun functionDeclaration() {

	}

	private fun variableDeclaration(): Unit? {
		expect(IDENTIFIER, "Expected variable name after '${previous.lexeme}'.") ?: return null
		val name = previous

		var type: Type = Type.None

		if (match(COLON)) {
			expect(IDENTIFIER, "Expected variable type after ':'.") ?: return null
			type = findType(previous) ?: return null
		}

		if (!match(EQUAL)) {
			scope.variables[name.lexeme] = Variable(type, false, false)
			return Unit
		}
	}

	private fun implDeclaration() {

	}

	private fun statement(): Unit? {
		return when (current.type) {
			else -> expressionStatement()
		}
	}

	private fun expressionStatement(): Unit? {
		val expression = expression() ?: return null
		expect(SEMICOLON, "Expected ';' after expression.") ?: return null
		return Unit
	}

	private fun expression() = parsePrecedence(Precedence.ASSIGNMENT)

	fun unary(u: Boolean): ExprData? {
		val operator = previous
		val expression = parsePrecedence(Precedence.UNARY) ?: return null
		val typeExpected = when (operator.type) {
			MINUS -> Type.Number
			BANG -> Type.Bool
			else -> throw IllegalArgumentException()
		}
		if (expression.type != typeExpected) return rangeError(expression.range, "Expected type '$typeExpected' but got '${expression.type}'.")
		// generate code
		expression.range = operator.range.first..previous.range.last
		return expression
	}

	fun parenthesis(u: Boolean): ExprData? {
		val p = previous
		if (match(RIGHT_PAREN)) return ExprData(Type.Unit, p.range.first..p.range.last)
		val expression = expression() ?: return null
		expect(RIGHT_PAREN, "Expected ')' after expression.") ?: return null
		expression.range = p.range.first..previous.range.last
		return expression
	}

	fun returnExpr(u: Boolean): ExprData? {
		val keyword = previous
		if (!isNext(RIGHT_PAREN, SEMICOLON, COMMA, RIGHT_BRACE, RIGHT_BRACKET, ELSE, ELIF)) // only tokens that can come after a return
			expression() ?: return null

		// generate code

		return ExprData(Type.Never, keyword.range.first..previous.range.last)
	}

	fun breakExpr(u: Boolean): ExprData? {
		TODO("not yet implemented")
	}

	fun variable(canAssign: Boolean): ExprData? {
		val name = previous
		if (match(LEFT_BRACE)) return init(name)

		val variable = findVariable(name) ?: return null

		if (!canAssign || !match(EQUAL))
			return if (!variable.initialized) {
				errorAt(name, "Variable '$name' was used before it was initialized.")
				null
			} else ExprData(variable.type, name.range)

		val expression = expression() ?: return null
		expression.range = name.range.first..previous.range.last
		if (expression.type != variable.type) return rangeError(
			expression.range,
			"Set type of '${expression.type}' does not match with the declared type of '${name.lexeme} (${variable.type})"
		)

		// generate code

		return expression
	}

	fun init(name: Token): ExprData? {
		val struct = findStruct(name) ?: return null
		val map = struct.fields.clone() as HashMap<String, Pair<Int, Type>>
		if (!isNext(RIGHT_BRACE)) {
			do {
				expect(IDENTIFIER, "Expected field name.") ?: return null
				val name = previous
				expect(COLON, "Expected ':' after field name.") ?: return null
				val expression = expression() ?: return null
				expression.range = name.range.first..previous.range.last
				val (i, type) = map[name.lexeme].also { map.remove(name.lexeme) } ?: return rangeError(
					expression.range,
					"Field '${name.lexeme}' in type '$struct' does not exist."
				)

				if (type != expression.type)
					return rangeError(
						expression.range,
						"Field '${name.lexeme}' in type '$struct' was set with type '${expression.type}' while it was declared with type '$type'."
					)
			} while (match(COMMA))
		}
		expect(RIGHT_BRACE, "Expect '}' after struct initialization.") ?: return null
		val expression = ExprData(struct.type, name.range.first..previous.range.last)
		if (map.size != 0) return rangeError(expression.range, "Missing fields ${
			run {
				val stringBuilder = StringBuilder()
				for ((k, v) in map.entries) {
					stringBuilder.append("'$k', ")
				}
				stringBuilder.substring(0, stringBuilder.length - 2)
				stringBuilder.toString()
			}
		} in initialization of struct '$struct'.")

		return expression
	}

	fun stringLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.String, previous.range)
	}

	fun charLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.Char, previous.range)
	}

	fun numberLiteral(u: Boolean): ExprData {
		parseDouble(previous.lexeme)
		// generate code
		return ExprData(Type.Number, previous.range)
	}

	fun trueLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.Bool, previous.range)
	}

	fun falseLiteral(u: Boolean): ExprData {
		// generate code
		return ExprData(Type.Bool, previous.range)
	}

	fun or(left: ExprData, u: Boolean) = binary(left, Precedence.AND, OP_OR, Type.Bool, Type.Bool)
	fun and(left: ExprData, u: Boolean) = binary(left, Precedence.EQUALITY, OP_AND, Type.Bool, Type.Bool)
	fun equality(left: ExprData, u: Boolean) = binary(
		left, Precedence.COMPARISON, when (previous.type) {
			EQUAL_EQUAL -> OP_EQUAL
			BANG_EQUAL -> OP_NOT_EQUAL
			else -> throw IllegalArgumentException()
		}, Type.Never, Type.Bool
	)

	fun comparison(left: ExprData, u: Boolean): ExprData? {
		val operator = previous
		val right = parsePrecedence(Precedence.TERM) ?: return null
		val range = left.range.first..right.range.last
		if (!(left.type == Type.Number && right.type == Type.Number)) {
			return rangeError(range, "Cannot perform binary '${operator.lexeme}' on types '${left.type}' and '${right.type}'.")
		}
		left.range = range
		left.type = Type.Bool
		// generate code
		return left
	}

	fun term(left: ExprData, u: Boolean) = binary(left, Precedence.FACTOR, if (previous.type == PLUS) OP_ADD else OP_SUB, Type.Number, Type.Number)
	fun factor(left: ExprData, u: Boolean) =
		binary(left, Precedence.EXPONENT, if (previous.type == STAR) OP_ADD else OP_DIV, Type.Number, Type.Number)

	fun exponent(left: ExprData, u: Boolean) = binary(left, Precedence.UNARY, OP_POW, Type.Number, Type.Number)

	fun call(callee: ExprData, u: Boolean): ExprData? {
		if (callee.type !is Type.Function) return rangeError(callee.range, "Cannot perform call on type '${callee.type}'.")

		var arity = 0
		val argArity = (callee.type as Type.Function).args.size - 1
		if (!isNext(RIGHT_PAREN)) {
			do {
				if (arity > argArity) {
					expression() ?: return null
					continue
				}

				val expression = expression() ?: return null
				val argType = (callee.type as Type.Function).args[arity++]
				if (argType != expression.type) return rangeError(callee.range, "Expected type '${expression.type}' but got '${argType}'.")
			} while (match(COMMA))

			if (arity != argArity) return rangeError(
				callee.range.first..previous.range.last,
				"Function has '$argArity' argument${if (argArity > 1) "s" else ""} but '${arity}' ${if (arity > 1) "were" else "was"} given."
			)
		}
		expect(RIGHT_PAREN, "Expected ')' after function arguments.") ?: return null
		callee.range = callee.range.first..previous.range.last
		callee.type = (callee.type as Type.Function).returnType
		return callee
	}

	fun dot(callee: ExprData, canAssign: Boolean): ExprData? {
		expect(IDENTIFIER, "Expected field name after '.'.") ?: return null
		val name = previous
		if (callee.type !is Type.Object) return rangeError(callee.range, "Cannot get field using '.' on type '${callee.type}'.")
		callee.range = callee.range.first..name.range.last
		callee.type = (callee.type as Type.Object).struct.fields[name.lexeme]?.second ?: return rangeError(
			callee.range,
			"Field '${name.lexeme}' in type '${callee.type}' does not exist."
		)

		if (!canAssign || !match(EQUAL)) return callee

		val expression = expression() ?: return null
		callee.range = callee.range.first..previous.range.last
		if (expression.type == callee.type) return callee

		return rangeError(callee.range, "Type '${expression.type}' does not match with expected type '${callee.type}'.")
	}


	private fun binary(left: ExprData, next: Precedence, opcode: Byte, type: Type, returnType: Type): ExprData? {
		val operator = previous
		val right = parsePrecedence(next) ?: return null
		val range = left.range.first..right.range.last
		if (!(left.type == type && right.type == type)) {
			return rangeError(range, "Cannot perform '${operator.lexeme}' on types '${left.type}' and '${right.type}'.")
		}
		left.range = range
		left.type = returnType
		// generate code
		return left
	}

	private fun parsePrecedence(precedence: Precedence): ExprData? {
		advance()
		val rule = previous.type.rule.prefix
		if (rule == null) {
			error("Expected expression.")
			return null
		}
		val canAssign = precedence.ordinal <= Precedence.ASSIGNMENT.ordinal
		var left = rule(canAssign) ?: return null
		while (precedence.ordinal <= current.type.rule.precedence.ordinal) {
			advance()
			val infix = previous.type.rule.infix!!
			left = infix(left, canAssign) ?: return null
		}
		if (canAssign && match(EQUAL)) {
			error("Invalid assignment target.")
			return null
		}
		return left
	}

	private fun findVariable(name: Token): Variable? {
		for (i in scopeDepth downTo 0) {
			val currentScope = scopes[i]!!
			return currentScope.variables[name.lexeme] ?: continue
		}
		errorAt(name, "Variable '${name.lexeme}' could not be found in the current scope.")
		return null
	}

	private fun findNamespace(name: Token): Scope? {
		for (i in scopeDepth downTo 0) {
			val currentScope = scopes[i]!!
			return currentScope.namespaces[name.lexeme] ?: continue
		}
		errorAt(name, "Namespace '${name.lexeme}' could not be found in the current scope.")
		return null
	}

	private fun findType(name: Token): Type? {
		for (i in scopeDepth downTo 0) {
			val currentScope = scopes[i]!!
			return currentScope.types[name.lexeme] ?: continue
		}
		errorAt(name, "Type '${name.lexeme}' could not be found in the current scope.")
		return null
	}

	private fun findStruct(name: Token): Struct? {
		for (i in scopeDepth downTo 0) {
			val currentScope = scopes[i]!!
			return currentScope.structs[name.lexeme] ?: continue
		}
		errorAt(name, "Struct '${name.lexeme}' could not be found in the current scope.")
		return null
	}

	private fun findTrait(name: Token): Trait? {
		for (i in scopeDepth downTo 0) {
			val currentScope = scopes[i]!!
			return currentScope.traits[name.lexeme] ?: continue
		}
		errorAt(name, "Trait '${name.lexeme}' could not be found in the current scope.")
		return null
	}


	private val lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()

	private fun advance(): Unit? {
		previous = current; current = lexer.scanToken(); return if (current.type == Token.Type.ERROR) {
			errorAtCurrent(current.lexeme); null
		} else Unit
	}

	private fun expect(type: Token.Type, message: String) = if (!match(type)) {
		errorAtCurrent(message); null
	} else Unit

	private fun match(type: Token.Type) = if (isNext(type)) {
		advance(); true
	} else false

	private fun match(types: Array<out Token.Type>) = if (current.type in types) {
		advance(); true
	} else false

	private fun end() = current.type == Token.Type.EOF
	private fun isNext(type: Token.Type) = current.type == type
	private fun isNext(vararg types: Token.Type) = current.type in types
	private fun error(message: String) = errorAt(previous, message)
	private fun errorAtCurrent(message: String) = errorAt(current, message)
	private fun errorAt(token: Token, message: String) = runtime.reportCompileError(message)

	private fun <T> rangeError(range: IntRange, message: String): T? {
		// TODO: do something here?
		return null
	}
}