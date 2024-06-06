package com.light672.zinc.lang.compiler.parsing.syntax

import com.light672.zinc.lang.compiler.parsing.syntax.tools.Either

internal sealed class Stmt {
	class Expression(val expr: Expr, val trailing: Boolean) : Stmt()
	class Function(
		val def: Token,
		val generics: GenericParams?,
		val name: Token,
		val params: FunctionParams?,
		val returnType: ReturnType?,
		val scOrBlock: Either<Token, Expr.Block>
	) : Stmt() {
		class FunctionParams(val params: ArrayList<FunctionParam>) {
			sealed class FunctionParam {
				class SelfParam(val mut: Token?, val self: Token) : FunctionParam()
				class NormalParam(val pattern: Pattern, val type: Type) : FunctionParam()
			}
		}

		class ReturnType(val colon: Token, val type: Type)
	}

	class Semicolon(val token: Token) : Stmt()
	class Variable(val pattern: Pattern, val type: Type?, val initializer: Expr?) : Stmt()
}