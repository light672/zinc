package com.light672.zinc.dsr

import com.light672.zinc.ast.FunctionParam as ASTFunctionParam
import com.light672.zinc.ast.Stmt as ASTStmt

internal sealed interface AssociatedStmt : Stmt


internal sealed interface Stmt {
	data class Function(
		val genericParams: GenericParams?,
		val parameterBranch: Branch<ValueItem>,
		val parameters: List<FunctionParam>,
		val block: Expr.Block?,
		val ast: ASTStmt.Function
	) : AssociatedStmt

	data class Trait(
		val genericParams: GenericParams?,
		val statements: List<AssociatedStmt>,
		val ast: ASTStmt.Trait
	) : Stmt

	data class Implementation(
		val genericParams: GenericParams?,
		val statements: List<AssociatedStmt>,
		val ast: ASTStmt.Implementation
	) : Stmt

	data class UnitStruct(
		val genericParams: GenericParams?,
		val ast: ASTStmt.UnitStruct
	) : Stmt

	data class TupleStruct(
		val genericParams: GenericParams?,
		val ast: ASTStmt.TupleStruct
	) : Stmt

	data class Struct(
		val genericParams: GenericParams?,
		val ast: ASTStmt.Struct
	) : Stmt

	data class Module(
		val types: Branch<TypeItem>,
		val values: Branch<ValueItem>,
		val statements: List<Stmt>,
		val ast: ASTStmt.Module
	) : Stmt

	data class Let(
		val branch: Branch<ValueItem>,
		val pattern: Pattern,
		val initializer: Expr?,
		val ast: ASTStmt.Let
	) : Stmt

	data class Expression(
		val expr: Expr,
		val ast: ASTStmt.Expression
	) : Stmt
}

internal sealed interface FunctionParam {
	data class Self(val item: com.light672.zinc.dsr.Self, val ast: ASTFunctionParam.Self) : FunctionParam
	data class Pattern(val pattern: com.light672.zinc.dsr.Pattern, val ast: ASTFunctionParam.Pattern) : FunctionParam
}