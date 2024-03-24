package com.light672.zinc.lang.compiler.parsing

internal sealed class Stmt {
	data class ExpressionStatement(val expression: Expr, val sc: Token?) : Stmt() {
		override val range = expression.range.first..(sc?.range?.last ?: expression.range.last)
		override fun toString() = "$expression;"
	}

	data class Function(
		val declaration: Token,
		val name: Token,
		val arguments: Array<Pair<Token, Token>>,
		val rightParen: Token,
		val type: Token?,
		val body: Array<Stmt>,
		val closeToken: Token,
	) : Stmt() {
		override val range = declaration.range.first..(type?.range?.last ?: rightParen.range.last)
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Function

			if (declaration != other.declaration) return false
			if (name != other.name) return false
			if (!arguments.contentEquals(other.arguments)) return false
			if (rightParen != other.rightParen) return false
			if (type != other.type) return false
			if (!body.contentEquals(other.body)) return false
			if (closeToken != other.closeToken) return false

			return true
		}

		override fun hashCode(): Int {
			var result = declaration.hashCode()
			result = 31 * result + name.hashCode()
			result = 31 * result + arguments.contentHashCode()
			result = 31 * result + rightParen.hashCode()
			result = 31 * result + (type?.hashCode() ?: 0)
			result = 31 * result + body.contentHashCode()
			result = 31 * result + closeToken.hashCode()
			return result
		}
	}

	data class Struct(
		val declaration: Token,
		val name: Token,
		val fields: Array<Pair<Token, Token>>,
		val closeToken: Token
	) : Stmt() {
		override val range = declaration.range.first..closeToken.range.last
		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (javaClass != other?.javaClass) return false

			other as Struct

			if (declaration != other.declaration) return false
			if (name != other.name) return false
			if (!fields.contentEquals(other.fields)) return false
			if (closeToken != other.closeToken) return false

			return true
		}

		override fun hashCode(): Int {
			var result = declaration.hashCode()
			result = 31 * result + name.hashCode()
			result = 31 * result + fields.contentHashCode()
			result = 31 * result + closeToken.hashCode()
			return result
		}
	}

	data class VariableDeclaration(val declaration: Token, val name: Token, val type: Token?, val initializer: Expr?, val sc: Token) : Stmt() {
		override val range = declaration.range.first..sc.range.last
	}

	abstract val range: IntRange
}