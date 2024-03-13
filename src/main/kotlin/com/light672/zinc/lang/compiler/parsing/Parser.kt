package com.light672.zinc.lang.compiler.parsing

import com.light672.zinc.Zinc
import com.light672.zinc.lang.compiler.CompilerError

internal sealed class Parser(source: String, val runtime: Zinc.Runtime) {
	private val lexer = Lexer(source)
	private var current: Token = Token.empty()
	private var previous: Token = Token.empty()

	abstract fun expression(): Expr?

	fun getNameAndType(variableType: String): Pair<Token, Token>? {
		expect(Token.Type.IDENTIFIER, "Expected $variableType name.") ?: return null
		val name = previous
		expect(Token.Type.COLON, "Expected ':' after $variableType name.") ?: return null
		expect(Token.Type.IDENTIFIER, "Expected $variableType type after ':'.") ?: return null
		return Pair(name, previous)
	}

	fun getNameAndExpression(variableType: String): Pair<Token, Expr>? {
		expect(Token.Type.IDENTIFIER, "Expected $variableType name.") ?: return null
		val name = previous
		expect(Token.Type.COLON, "Expected ':' after $variableType name.") ?: return null
		val expression = expression() ?: return null
		return Pair(name, expression)
	}

	fun advance(): Unit? {
		previous = current; current = lexer.scanToken(); return if (current.type == Token.Type.ERROR) {
			errorAtCurrent(current.lexeme); null
		} else Unit
	}

	fun expect(type: Token.Type, message: String) = if (!match(type)) {
		errorAtCurrent(message); null
	} else Unit

	fun match(type: Token.Type) = if (isNext(type)) {
		advance(); true
	} else false

	fun match(types: Array<out Token.Type>) = if (current.type in types) {
		advance(); true
	} else false

	fun end() = current.type == Token.Type.EOF
	fun isNext(type: Token.Type) = current.type == type
	fun isNext(vararg types: Token.Type) = current.type in types
	fun error(message: String) = errorAt(previous, message)
	fun errorAtCurrent(message: String) = errorAt(current, message)
	fun errorAt(token: Token, message: String) = runtime.reportCompileError(CompilerError.TokenError(token, message))
}