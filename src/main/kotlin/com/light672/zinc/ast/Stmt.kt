package com.light672.zinc.ast

internal sealed interface Stmt {
	data class Function(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val params: List<FunctionParam>,
		val returnType: Type?,
		val block: Expr.Block
	) : Stmt

	data class FunctionNoBlock(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val params: List<FunctionParam>,
		val returnType: Type?,
		val semicolon: Token
	) : Stmt

	data class UnitStruct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val semicolon: Token
	) : Stmt

	data class TupleStruct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val fields: List<Type>,
		val close: Token
	) : Stmt

	data class Struct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val fields: List<Pair<Token, Type>>,
		val close: Token
	) : Stmt

	data class Module(
		val keyword: Token,
		val name: Token,
		val statements: List<Stmt>,
		val close: Token
	) : Stmt

	data class Let(
		val keyword: Token,
		val pattern: Pattern,
		val type: Type?,
		val initializer: Expr?,
	) : Stmt

	data class Expression(val expr: Expr, val semicolon: Token?) : Stmt
}

internal sealed interface FunctionParam {
	data class SelfParam(val self: Token, val type: Type?) : FunctionParam
	data class PatternParam(val pattern: Pattern, val type: Type) : FunctionParam
}