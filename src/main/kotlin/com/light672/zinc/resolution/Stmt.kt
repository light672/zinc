package com.light672.zinc.resolution

import com.light672.zinc.ast.FunctionParam as ASTFunctionParam
import com.light672.zinc.ast.Stmt as ASTStmt

internal sealed interface Stmt {
	data class Function(
		val genericParams: GenericParams?,
		val params: List<FunctionParam>,
		val returnType: Type?,
		val block: Expr.Block,
		val ast: ASTStmt.Function
	) : Stmt

	data class FunctionNoBlock(
		val genericParams: GenericParams?,
		val params: List<FunctionParam>,
		val returnType: Type?,
		val ast: ASTStmt.FunctionNoBlock
	) : Stmt

	data class UnitStruct(
		val genericParams: GenericParams?,
		val ast: ASTStmt.UnitStruct
	) : Stmt

	data class TupleStruct(
		val genericParams: GenericParams?,
		val fields: List<Type>,
		val ast: ASTStmt.TupleStruct
	) : Stmt

	data class Struct(
		val genericParams: GenericParams?,
		val fields: Map<CharSequence, Type>,
		val ast: ASTStmt.Struct
	) : Stmt

	data class Module(
		val statements: List<Stmt>,
		val ast: ASTStmt.Module
	) : Stmt

	data class Let(
		val pattern: Pattern,
		val type: Type?,
		val initializer: Expr?
	) : Stmt

	data class Expression(
		val expression: Expr,
		val ast: ASTStmt.Expression
	) : Stmt


}

internal interface FunctionParam {
	data class SelfParam(val type: Type?, val ast: ASTFunctionParam.SelfParam) : FunctionParam
	data class PatternParam(val pattern: Pattern, val type: Type) : FunctionParam
}