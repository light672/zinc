package com.light672.zinc.hir

import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Expr as ASTExpr


internal sealed interface Expr {
	data class Literal(
		val ast: ASTExpr.Literal
	) : Expr

	data class Item(
		val variable: ValueItemRef?,
		val ast: ASTExpr.Path
	) : Expr

	data class Tuple(
		val exprs: List<Expr>,
		val ast: ASTExpr.Group
	) : Expr

	data class FieldGet(
		val callee: Expr,
		val identifier: Token,
		val genericArgs: GenericArgs?,
		val ast: ASTExpr.FieldGet
	) : Expr

	data class MethodCall(
		val callee: Expr,
		val identifier: Token,
		val genericArgs: GenericArgs?,
		val args: List<Expr>,
		val ast: ASTExpr.Call
	) : Expr


	data class Call(
		val callee: Expr,
		val args: List<Expr>,
		val ast: ASTExpr.Call
	) : Expr

	data class Index(
		val callee: Expr,
		val args: List<Expr>,
		val ast: ASTExpr.Index
	) : Expr

	data class Unary(
		val right: Expr,
		val ast: ASTExpr.Unary
	) : Expr

	data class Range(
		val left: Expr?,
		val right: Expr?,
		val ast: ASTExpr.Range
	) : Expr

	data class Binary(
		val left: Expr,
		val right: Expr,
		val ast: ASTExpr.Binary
	) : Expr

	data class Closure(
		val patterns: List<Pattern>,
		val expr: Expr,
		val ast: ASTExpr.Closure
	) : Expr

	// TODO: add function to return from
	data class Return(
		val value: Expr?,
		val ast: ASTExpr.Return
	) : Expr

	// TODO: add labels to break and continue
	data class Break(
		val value: Expr?,
		val ast: ASTExpr.Break
	) : Expr

	data class Continue(
		val ast: ASTExpr.Continue
	) : Expr

	data class Block(
		val stmts: List<Stmt>,
		val ast: ASTExpr.Block
	) : Expr

	data class If(
		val condition: Expr,
		val then: Block,
		val orElse: Expr?,
		val ast: ASTExpr.If
	) : Expr

	data class Match(
		val expr: Expr,
		val branches: List<Pair<Pattern, Expr>>,
		val ast: ASTExpr.Match
	) : Expr

	data class Loop(
		val block: Block,
		val ast: ASTExpr.Loop
	) : Expr

	data class While(
		val condition: Expr,
		val block: Block,
		val ast: ASTExpr.While
	) : Expr

	data class For(
		val pattern: Pattern,
		val iterator: Expr,
		val block: Block,
		val ast: ASTExpr.For
	) : Expr


}