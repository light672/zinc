package com.light672.zinc.resolution

import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Expr as ASTExpr

internal sealed interface Expr {
	data class Literal(val ast: ASTExpr) : Expr
	data class Item(val item: ValueItemReference?, val ast: ASTExpr) : Expr
	data class Tuple(val expressions: List<Expr>, val ast: ASTExpr) : Expr
	data class FieldGet(val callee: Expr, val segment: Pair<Token, GenericArgs?>, val ast: ASTExpr) : Expr
	data class Call(val callee: Expr, val args: List<Expr>, val ast: ASTExpr) : Expr
	data class Index(val callee: Expr, val args: List<Expr>, val ast: ASTExpr) : Expr
	data class Unary(val operator: Token, val right: Expr, val ast: ASTExpr) : Expr
	data class Range(val left: Expr?, val right: Expr?, val inclusive: Boolean, val ast: ASTExpr) : Expr
	data class Binary(val left: Expr, val operator: Token, val right: Expr, val ast: ASTExpr) : Expr
	data class Closure(val parameters: List<Pair<Pattern, Type?>>, val expr: Expr, val ast: ASTExpr) : Expr
	data class Return(val expr: Expr?, val ast: ASTExpr) : Expr
	data class Break(val expr: Expr?, val ast: ASTExpr) : Expr

	data class Block(val stmts: List<Stmt>, val ast: ASTExpr) : Expr
	data class If(val condition: Expr, val then: Block, val elseBlock: Expr?, val ast: ASTExpr) : Expr
	data class While(val condition: Expr, val block: Block, val ast: ASTExpr) : Expr
	data class For(val pattern: Pattern, val iterator: Expr, val block: Block, val ast: ASTExpr) : Expr
	data class Match(val expr: Expr, val branches: List<Pair<Pattern, Expr>>, val ast: ASTExpr) : Expr
	data class Loop(val block: Block, val ast: ASTExpr) : Expr
}