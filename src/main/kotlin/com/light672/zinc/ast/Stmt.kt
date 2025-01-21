package com.light672.zinc.ast

import com.light672.zinc.resolution.Scope
import com.light672.zinc.resolution.TypeItem
import com.light672.zinc.resolution.ValueItem

internal sealed interface Stmt {
	data class Function(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val params: List<FunctionParam>,
		val paramClose: Token,
		val returnType: Type?,
		// val whereClause: WhereClause?,
		val block: Expr.Block
	) : Stmt {
		lateinit var item: ValueItem.Function
		lateinit var genericScope: Scope
	}

	data class FunctionNoBlock(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val params: List<FunctionParam>,
		val paramClose: Token,
		val returnType: Type?,
		// val whereClause: WhereClause?,
		val semicolon: Token
	) : Stmt {
		lateinit var item: ValueItem.Function
		lateinit var genericScope: Scope
	}

	data class UnitStruct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		// val whereClause: WhereClause?,
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
		// val whereClause: WhereClause?,
		val close: Token
	) : Stmt {
		lateinit var item: TypeItem.TupleStruct
		lateinit var genericScope: Scope
	}

	data class Struct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val fields: List<Pair<Token, Type>>,
		// val whereClause: WhereClause?,
		val close: Token
	) : Stmt {
		lateinit var item: TypeItem.Struct
		lateinit var genericScope: Scope
	}

	data class Module(
		val keyword: Token,
		val name: Token,
		val statements: List<Stmt>,
		val close: Token
	) : Stmt {
		lateinit var item: TypeItem.Module
	}

	data class Let(
		val keyword: Token,
		val pattern: Pattern,
		val type: Type?,
		val initializer: Expr?,
	) : Stmt

	data class Expression(val expr: Expr, val semicolon: Token?) : Stmt

	fun range() = when (this) {
		is Expression -> expr.range().let { it.start..(semicolon?.asRange()?.end ?: it.end) }
		is Function -> keyword..(returnType?.range()?.end ?: paramClose)
		is FunctionNoBlock -> keyword..(returnType?.range()?.end ?: paramClose)
		is Let -> keyword..(initializer?.range()?.end ?: type?.range()?.end ?: pattern.range().end)
		is Module -> keyword..name
		is Struct -> keyword..(genericParams?.end ?: name)
		is TupleStruct -> keyword..(genericParams?.end ?: name)
		is UnitStruct -> keyword..(genericParams?.end ?: name)
	}
}

internal sealed interface FunctionParam {
	data class SelfParam(val self: Token, val type: Type?) : FunctionParam
	data class PatternParam(val pattern: Pattern, val type: Type) : FunctionParam
}