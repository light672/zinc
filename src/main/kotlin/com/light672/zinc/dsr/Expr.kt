package com.light672.zinc.dsr

import com.light672.zinc.ast.Expr as ASTExpr


internal sealed interface JumpableExpr : Expr
internal sealed interface Expr {
	data class Literal(
		val ast: ASTExpr.Literal
	) : Expr

	data class Path(
		val ast: ASTExpr.Path
	) : Expr

	data class Group(
		val exprs: List<Expr>,
		val ast: ASTExpr.Group
	) : Expr

	data class FieldGet(
		val callee: Expr,
		val ast: ASTExpr.FieldGet
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
		val paramScope: Branch<ValueItem>,
		val patterns: List<Pattern>,
		val expr: Expr,
		val ast: ASTExpr.Closure
	) : Expr

	data class Return(
		val value: Expr?,
		val ast: ASTExpr.Return
	) : Expr

	data class Break(
		val value: Expr?,
		val ast: ASTExpr.Break
	) : Expr

	data class Continue(
		val ast: ASTExpr.Continue
	) : Expr

	data class Block(
		val types: Branch<TypeItem>,
		val valeus: Branch<ValueItem>,
		val stmts: List<Stmt>,
		val ast: ASTExpr.Block
	) : JumpableExpr

	data class If(
		val condition: Expr,
		val then: Block,
		val orElse: Expr?,
		val ast: ASTExpr.If
	) : JumpableExpr

	data class While(
		val condition: Expr,
		val then: Block,
		val ast: ASTExpr.While
	) : JumpableExpr

	data class For(
		val patternScope: Branch<ValueItem>,
		val pattern: Pattern,
		val iterator: Expr,
		val block: Block,
		val ast: ASTExpr.For
	) : JumpableExpr

	data class Match(
		val expr: Expr,
		val branches: List<Triple<Branch<ValueItem>, Pattern, Expr>>,
		val ast: ASTExpr.Match
	) : Expr

	data class Loop(
		val block: Block,
		val ast: ASTExpr.Loop
	) : JumpableExpr
}