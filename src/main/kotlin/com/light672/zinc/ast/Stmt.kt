package com.light672.zinc.ast

import com.light672.zinc.Either
import com.light672.zinc.Scope
import com.light672.zinc.item.TypeItem
import com.light672.zinc.item.ValueItem

internal sealed interface Stmt {
	class Let(
		val keyword: Token,
		val pattern: Pattern,
		val type: Type?,
		val initializer: Expr?
	) : Stmt

	class Expression(
		val expr: Expr,
		val trailing: Boolean
	) : Stmt

	class Function(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val parameters: List<Pair<Pattern, Type>>,
		val returnType: Type?,
		val blockOrSemi: Either<Expr.Block, Token>,
		val item: ValueItem.Function
	) : Stmt

	class Interface(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val functions: List<Function>,
		val item: TypeItem.Interface
	) : Stmt

	class Struct(
		val keyword: Token,
		val name: Token,
		val genericParams: GenericParams?,
		val fields: List<Pair<Token, Type>>,
		val item: TypeItem.Struct
	) : Stmt

	class Module(
		val keyword: Token,
		val name: Token,
		val scope: Scope,
		val statements: List<Stmt>,
		val item: TypeItem.Module
	) : Stmt

	class Implementation(
		val genericParams: GenericParams?,
		val type: Type,
		val implInterface: Type?,
		val functions: List<Function>,
	) : Stmt
}