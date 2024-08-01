package com.light672.zinc.lang.compiler.debug

import com.light672.zinc.builtin.ZincBoolean
import com.light672.zinc.builtin.ZincChar
import com.light672.zinc.builtin.ZincNumber
import com.light672.zinc.builtin.ZincString
import com.light672.zinc.lang.compiler.ast.syntax.*
import com.light672.zinc.lang.tool.Either

internal object ASTPrinter {
	fun print(tree: Stmt.Module) = print(tree, "", Pair("", ""))

	private fun print(stmt: Stmt, prefix: String, indent: Pair<String, String>) {
		print(prefix + indent.first + stmtName(stmt))
		when (stmt) {
			is Stmt.Module -> {
				println()
				printStatementBody(stmt.stmts, prefix + indent.second)
			}

			is Stmt.Expression -> {
				if (!stmt.trailing) print(";")
				println()
				print(stmt.expr, prefix + indent.second)
			}

			is Stmt.Function -> if (stmt.scOrBlock is Either.Left) println(';') else {
				println()
				printStatementBody((stmt.scOrBlock as Either.Right).value.stmts, prefix + indent.second)
			}

			is Stmt.TypeAlias -> println()

			is Stmt.Variable -> {
				println()
				if (stmt.initializer != null) print(stmt.initializer, prefix + indent.second)
			}
		}
	}

	private fun print(expr: Expr, prefix: String) {
		when (expr) {
			is Expr.Binary -> {
				println(prefix + INDENT_END.first + "binary")
				print(expr.a, prefix + INDENT_END.second)
				print(expr.b, prefix + INDENT_END.second)
				println(prefix + INDENT_END.second + expr.operator.lexeme)
			}

			is Expr.Block -> {
				println(prefix + INDENT_END.first + "block")
				printStatementBody(expr.stmts, prefix + INDENT_END.second)
			}

			is Expr.Group -> {
				println(prefix + INDENT_END.first + "group")
				print(expr.expr, prefix + INDENT_END.second)
			}

			is Expr.Literal -> {
				println(
					prefix + INDENT_END.first + when (expr.literal) {
						is ZincBoolean -> expr.literal.value
						is ZincChar -> "'${expr.literal.value}'"
						is ZincNumber -> expr.literal.value
						is ZincString -> "\"${expr.literal.value}\""
						else -> throw IllegalArgumentException()
					}
				)
			}

			is Expr.Path -> println(prefix + INDENT_END.first + complexPath(expr.path))
			is Expr.Unary -> {
				println(prefix + INDENT_END.first + "unary")
				print(expr.a, prefix + INDENT_END.second)
				println(prefix + INDENT_END.second + expr.operator.lexeme)
			}
		}
	}

	private fun stmtName(stmt: Stmt): String {
		return when (stmt) {
			is Stmt.Module -> "mod ${stmt.name.lexeme}"
			is Stmt.Expression -> "expr"
			is Stmt.Function -> functionName(stmt)
			is Stmt.TypeAlias -> typeAliasName(stmt)
			is Stmt.Variable -> "let " + variableName(stmt)
		}
	}

	private fun functionName(function: Stmt.Function): String {
		val stringBuilder = StringBuilder("fn ${function.name.lexeme}")
		stringBuilder.append(genericParams(function.generics))

		stringBuilder.append("(")
		for (param in function.params) {
			when (param) {
				is Stmt.Function.NormalParam -> stringBuilder.append("${pattern(param.pattern)}: ${type(param.type)}")
				is Stmt.Function.SelfParam -> stringBuilder.append("self")
			}
			if (param !== function.params.last()) stringBuilder.append(", ")
		}
		stringBuilder.append(")")

		return stringBuilder.toString()
	}

	private fun typeAliasName(type: Stmt.TypeAlias): String {
		val stringBuilder = StringBuilder("typealias ${type.name.lexeme}")
		stringBuilder.append(genericParams(type.genericParams))
		if (type.typeParamBounds != null) stringBuilder.append(": ${typeBounds(type.typeParamBounds)}")
		if (type.type != null) stringBuilder.append(" = ${type(type.type)}")
		return stringBuilder.toString()
	}

	private fun variableName(variable: Stmt.Variable): String {
		val stringBuilder = StringBuilder(pattern(variable.pattern))
		if (variable.type != null)
			stringBuilder.append(": ${type(variable.type)}")
		return stringBuilder.toString()
	}

	private fun genericParams(generics: GenericParams?): String {
		generics ?: return ""
		val stringBuilder = StringBuilder("<")
		for (generic in generics.params) {
			stringBuilder.append(generic.name.lexeme)
			if (generic.bounds != null) stringBuilder.append(typeBounds(generic.bounds))
			if (generic === generics.params.last())
				stringBuilder.append(">")
			else
				stringBuilder.append(", ")
		}
		return stringBuilder.toString()
	}

	private fun genericArgs(generics: GenericArgs?): String {
		generics ?: return ""
		val stringBuilder = StringBuilder("<")
		for (generic in generics.args) {
			stringBuilder.append(type(generic))
			if (generic === generics.args.last())
				stringBuilder.append(">")
			else
				stringBuilder.append(", ")
		}
		return stringBuilder.toString()
	}

	private fun typeBounds(typeParamBounds: TypeParamBounds): String {
		return complexPath(typeParamBounds.path)
	}

	private fun complexPath(path: ComplexPath): String {
		val stringBuilder = StringBuilder()
		for (segment in path.tail) {
			stringBuilder.append(segment.segment.lexeme)
			stringBuilder.append(genericArgs(segment.generics))
			stringBuilder.append("::")
		}
		stringBuilder.append(path.head.segment.lexeme)
		stringBuilder.append(genericArgs(path.head.generics))
		return stringBuilder.toString()
	}

	private fun type(type: Type): String {
		return when (type) {
			is ComplexPath -> complexPath(type)
		}
	}

	private fun pattern(pattern: Pattern): String {
		return when (pattern) {
			is Pattern.IdentifierPattern -> "${(pattern.mut?.lexeme?.plus(" ")) ?: ""}${pattern.name.lexeme}"
			is Pattern.PathPattern -> complexPath(pattern.path.path)
		}
	}

	private fun printStatementBody(stmts: List<Stmt>, prefix: String) {
		for (stmt in stmts) {
			if (stmt === stmts.last()) {
				print(stmt, prefix, INDENT_END)
			} else {
				print(stmt, prefix, INDENT_MID)
			}
		}
	}
}