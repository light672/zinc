package com.light672.zinc.ast

internal sealed interface Expr {
	data class Literal(val token: Token) : Expr
	data class Path(val path: ComplexPath) : Expr
	data class Variable(val identifier: Token) : Expr
	data class Group(val open: Token, val expressions: List<Expr>, val close: Token) : Expr
	data class FieldGet(val callee: Expr, val segment: ComplexSegment) : Expr
	data class Call(val callee: Expr, val argOpen: Token, val args: List<Expr>, val argClose: Token) : Expr
	data class Index(val callee: Expr, val argOpen: Token, val args: List<Expr>, val argClose: Token) : Expr
	data class Unary(val operator: Token, val right: Expr) : Expr
	data class Range(val left: Expr?, val operator: Token, val right: Expr?) : Expr
	data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr
	data class Closure(val open: Token, val params: List<Pair<Pattern, Type?>>, val close: Token, val type: Type?, val expr: Expr) : Expr
	data class Return(val keyword: Token, val expr: Expr?) : Expr
	data class Break(val keyword: Token, val expr: Expr?) : Expr

	data class Block(val start: Token, val stmts: List<Stmt>, val end: Token) : Expr
	data class If(val keyword: Token, val condition: Expr, val thenBlock: Block, val elseExpr: Expr?) : Expr
	data class While(val keyword: Token, val condition: Expr, val block: Block) : Expr
	data class For(val keyword: Token, val pattern: Pattern, val iterator: Expr, val block: Block) : Expr
	data class Match(val keyword: Token, val expr: Expr, val branches: List<Pair<Pattern, Expr>>, val close: Token) : Expr
	data class Loop(val keyword: Token, val block: Block) : Expr


	fun range(): Token.Range {
		return when (this) {
			is Binary -> left.range().start..right.range().end
			is Break -> keyword..(expr?.range()?.end ?: keyword)
			is Call -> callee.range().start..argClose
			is Closure -> open..expr.range().end
			is Group -> open..close
			is Index -> callee.range().start..argClose
			is Literal -> token.asRange()
			is Path -> when (path) {
				is ComplexPath.Normal -> path.body.first().id..path.body.last().let { last -> last.generics?.end ?: last.id }
				is ComplexPath.Qualified -> path.start..path.body.last().let { last -> last.generics?.end ?: last.id }
				is ComplexPath.Error -> path.range
			}

			is Range -> (left?.range()?.start ?: operator)..(right?.range()?.end ?: operator)
			is Return -> keyword..(expr?.range()?.end ?: keyword)
			is Unary -> operator..right.range().end
			is Variable -> identifier.asRange()

			is Block -> start..end
			is If -> keyword..(elseExpr?.range()?.end ?: thenBlock.end)
			is While -> keyword..block.end
			is Loop -> keyword..block.end
			is For -> keyword..block.end
			is Match -> keyword..close
			is FieldGet -> callee.range().start..(segment.generics?.end ?: segment.id)
		}
	}

	fun name() = when (this) {
		is Binary -> "binary expression"
		is Break -> "break expression"
		is Call -> "call expression"
		is Closure -> "closure"
		is Group -> "group expression"
		is Index -> "indexing expression"
		is Literal -> "literal expression"
		is Path -> "path expression"
		is Range -> "range expression"
		is Return -> "return expression"
		is Unary -> "unary expression"
		is Variable -> "variable expression"
		is Block -> "block expression"
		is If -> "if expression"
		is While -> "while expression"
		is Loop -> "loop expression"
		is For -> "for expression"
		is Match -> "match expression"
		is FieldGet -> "field access"
	}


}