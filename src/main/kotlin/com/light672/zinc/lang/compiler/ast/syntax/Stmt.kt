package com.light672.zinc.lang.compiler.ast.syntax

import com.light672.zinc.lang.tool.Either

internal sealed class Stmt(val keyword: Token, val lastToken: Token) {

	class Expression(val expr: Expr, val trailing: Boolean, semicolon: Token?) : Stmt(expr.firstToken, semicolon ?: expr.lastToken)

	class Module(
		keyword: Token,
		val name: Token,
		val stmts: ArrayList<Stmt>
	) : Stmt(keyword, name)

	class Function(
		keyword: Token,
		val name: Token,
		val generics: GenericParams?,
		val params: List<FunctionParam>,
		paramClose: Token,
		val returnType: Type?,
		val scOrBlock: Either<Token, Expr.Block>
	) : Stmt(
		keyword, when (scOrBlock) {
			is Either.Left -> scOrBlock.value
			is Either.Right -> returnType?.range()?.last ?: paramClose
		}
	) {
		sealed class FunctionParam
		class SelfParam(val mut: Token?, val self: Token) : FunctionParam()
		class NormalParam(val pattern: Pattern, val type: Type) : FunctionParam()
	}

	class Variable(keyword: Token, val pattern: Pattern, val type: Type?, val initializer: Expr?, semicolon: Token) : Stmt(keyword, semicolon)
	class TypeAlias(
		keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val typeParamBounds: TypeParamBounds?,
		val type: Type?,
		semicolon: Token
	) : Stmt(keyword, semicolon)

	fun range() = keyword..lastToken
}