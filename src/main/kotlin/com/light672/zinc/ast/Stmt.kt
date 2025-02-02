package com.light672.zinc.ast

import com.light672.zinc.resolution.Scope
import com.light672.zinc.resolution.TypeItem

internal sealed interface Stmt {
	data class Function(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val params: List<FunctionParam>,
		val paramClose: Token,
		val returnType: Type?,
		val whereClause: WhereClause?,
		val block: Expr.Block?
	) : AssociatedStmt

	data class Trait(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val whereClause: WhereClause?,
		val statements: List<AssociatedStmt>
	) : Stmt

	data class Implementation(
		val keyword: Token,
		val genericParams: GenericParams?,
		val type: Type,
		val trait: ComplexPath.Normal?,
		val whereClause: WhereClause?,
		val statements: List<AssociatedStmt>
	) : Stmt

	data class UnitStruct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val whereClause: WhereClause?,
		val semicolon: Token
	) : Stmt {
		lateinit var item: TypeItem.UnitStruct
		lateinit var genericScope: Scope
	}

	data class TupleStruct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val fields: List<Type>,
		val whereClause: WhereClause?,
		val close: Token
	) : Stmt

	data class Struct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val fields: List<Pair<Token, Type>>,
		val whereClause: WhereClause?,
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

	// TODO: include where clauses in range
	fun range() = when (this) {
		is Expression -> expr.range().let { it.start..(semicolon?.asRange()?.end ?: it.end) }
		is Function -> keyword..(returnType?.range()?.end ?: paramClose)
		is Let -> keyword..(initializer?.range()?.end ?: type?.range()?.end ?: pattern.range().end)
		is Module -> keyword..name
		is Struct -> keyword..(genericParams?.end ?: name)
		is TupleStruct -> keyword..(genericParams?.end ?: name)
		is UnitStruct -> keyword..(genericParams?.end ?: name)
		is Implementation -> keyword..(trait?.range()?.end ?: type.range().end)
		is Trait -> keyword..(genericParams?.end ?: name)
	}
}

internal sealed interface FunctionParam {
	data class Self(val self: Token, val type: Type?) : FunctionParam
	data class Pattern(val pattern: com.light672.zinc.ast.Pattern, val type: Type) : FunctionParam
}

internal sealed interface AssociatedStmt : Stmt