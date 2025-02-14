package com.light672.zinc.hir

import com.light672.zinc.ast.Token
import com.light672.zinc.dsr.GenericParams
import com.light672.zinc.ast.FunctionParam as ASTFunctionParam
import com.light672.zinc.ast.Stmt as ASTStmt
import com.light672.zinc.dsr.FunctionParam as DSRFunctionParam

internal sealed interface AssociatedStmt : Stmt

internal sealed interface Stmt {
	data class Function(
		val generics: GenericParams?,
		val whereClause: WhereClause,
		val parameters: List<FunctionParam>,
		val block: Expr.Block?,
		val ast: ASTStmt.Function
	) : AssociatedStmt

	data class InherentImpl(
		val generics: GenericParams?,
		val whereClause: WhereClause,
		val type: Type,
		val statements: List<AssociatedStmt>,
		val ast: ASTStmt.Implementation
	) : Stmt

	data class TraitImpl(
		val generics: GenericParams?,
		val whereClause: WhereClause,
		val type: Type,
		val trait: TypeItemRef.Normal?,
		val statements: List<AssociatedStmt>,
		val ast: ASTStmt.Implementation
	) : Stmt

	data class Trait(
		val generics: GenericParams?,
		val whereClause: WhereClause,
		val statements: List<AssociatedStmt>,
		val ast: ASTStmt.Trait
	) : Stmt

	data class Expression(
		val expr: Expr,
		val ast: ASTStmt.Expression
	) : Stmt

	data class Let(
		val pattern: Pattern,
		val type: Type?,
		val initializer: Expr?,
		val ast: ASTStmt.Let
	) : Stmt

	data class Module(
		val statements: List<Stmt>
	) : Stmt

	data class Struct(
		val generics: GenericParams?,
		val whereClause: WhereClause,
		val fields: List<Pair<Token, Type>>,
		val ast: ASTStmt.Struct
	) : Stmt

	data class TupleStruct(
		val generics: GenericParams?,
		val whereClause: WhereClause,
		val fields: List<Type>,
		val ast: ASTStmt.TupleStruct
	) : Stmt

	data class UnitStruct(
		val generics: GenericParams?,
		val whereClause: WhereClause,
		val ast: ASTStmt.UnitStruct
	) : Stmt
}

internal sealed interface FunctionParam {
	data class Self(val dsr: DSRFunctionParam, val type: Type?) : FunctionParam
	data class Pattern(val pattern: com.light672.zinc.hir.Pattern, val type: Type, val ast: ASTFunctionParam) : FunctionParam
}