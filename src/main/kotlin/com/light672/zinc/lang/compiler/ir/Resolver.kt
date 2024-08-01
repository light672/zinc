package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.Zinc
import com.light672.zinc.lang.compiler.ast.syntax.*
import com.light672.zinc.lang.tool.Either

internal class Resolver(private val namespace: Namespace, private val zinc: Zinc.Runtime) {
	private var currentModule: Module = namespace.rootModule
	fun resolveAndLower() {
		for (type in namespace.types) {
			resolve(type)
		}
	}


	private fun resolve(item: Item) {
		when (item) {
			is Function -> resolveFunction(item)
			is Module -> TODO()
			is TypeAlias -> TODO()
			is Variable -> TODO()
		}
	}

	private fun resolveFunction(function: Function) {
		val params = function.declaration.params.map { param ->
			when (param) {
				is Stmt.Function.NormalParam -> {
					resolveType(param.type)
				}

				is Stmt.Function.SelfParam -> {
					zinc.reportCompileError(
						"Function parameter 'self' is not allowed in non-associated functions.",
						(param.mut ?: param.self)..param.self
					)
					Type.ERROR
				}
			}
		}
		function.parameters = params
		val returnType = resolveType(function.declaration.returnType)
		function.returnType = returnType
		when (function.declaration.scOrBlock) {
			is Either.Left -> {
				function.block = IRExpr.Block(emptyList())
				return
			}

			is Either.Right -> {
				val block = function.declaration.scOrBlock
				function.block = lowerASTExpr(block.value) as IRExpr.Block
			}
		}
	}

	private fun lowerASTStmt(stmt: Stmt): IRStmt? {
		return when (stmt) {
			is Stmt.Module, is Stmt.Function, is Stmt.TypeAlias -> null
			is Stmt.Expression -> IRStmt.Expr(lowerASTExpr(stmt.expr), stmt.trailing)
			is Stmt.Variable -> TODO()
		}
	}

	private fun lowerASTExpr(expr: Expr): IRExpr {
		return when (expr) {
			is Expr.Binary -> IRExpr.Binary(
				lowerASTExpr(expr.a), lowerASTExpr(expr.b), when (expr.operator.type) {
					Token.Type.PLUS -> IRExpr.Binary.BinaryOp.ADD
					Token.Type.MINUS -> IRExpr.Binary.BinaryOp.SUBTRACT
					Token.Type.SLASH -> IRExpr.Binary.BinaryOp.DIVIDE
					Token.Type.STAR -> IRExpr.Binary.BinaryOp.MULTIPLY
					Token.Type.PERCENT -> IRExpr.Binary.BinaryOp.MODULO
					Token.Type.GREATER -> IRExpr.Binary.BinaryOp.GREATER
					Token.Type.GREATER_EQUAL -> IRExpr.Binary.BinaryOp.GREATER_EQUAL
					Token.Type.LESS -> IRExpr.Binary.BinaryOp.LESS
					Token.Type.LESS_EQUAL -> IRExpr.Binary.BinaryOp.LESS_EQUAL
					Token.Type.EQUAL_EQUAL -> IRExpr.Binary.BinaryOp.EQUAL
					Token.Type.AMP -> IRExpr.Binary.BinaryOp.BIT_AND
					Token.Type.PIPE -> IRExpr.Binary.BinaryOp.BIT_OR
					Token.Type.CARET -> IRExpr.Binary.BinaryOp.BIT_XOR
					Token.Type.PIPE_PIPE -> IRExpr.Binary.BinaryOp.OR
					Token.Type.AMP_AMP -> IRExpr.Binary.BinaryOp.AND
					else -> throw IllegalArgumentException()
				}
			)

			is Expr.Block -> IRExpr.Block(ArrayList<IRStmt>(expr.stmts.size).also {
				for (stmt in expr.stmts)
					lowerASTStmt(stmt)?.let { stmt -> it.add(stmt) }
			})

			is Expr.Group -> lowerASTExpr(expr.expr)
			is Expr.Literal -> IRExpr.Literal(expr.literal)

			is Expr.Path -> resolveComplexPathExpr(expr.path)?.let { IRExpr.Variable(it) } ?:
			is Expr.Unary -> IRExpr.Unary(
				lowerASTExpr(expr.a), when (expr.operator.type) {
					Token.Type.MINUS -> IRExpr.Unary.UnaryOp.NEGATE
					Token.Type.BANG -> IRExpr.Unary.UnaryOp.NOT
					// Token.Type.TILDA -> IRExpr.Unary.UnaryOp.Invert
					else -> throw IllegalArgumentException()
				}
			)
		}
	}

	private fun resolveType(type: com.light672.zinc.lang.compiler.ast.syntax.Type?): Type {
		return when (type) {
			is ComplexPath -> {
				val (item, genericArgs) = resolveComplexPathType(type) ?: return Type.ERROR
				when (item) {
					is Function, is Variable -> throw IllegalArgumentException("This will never happen")
					is Module -> {
						zinc.reportCompileError("Expected type, got ", (type.tail.firstOrNull()?.segment ?: type.head.segment)..type.head.segment)
						Type.ERROR
					}

					is TypeAlias -> Type(item, genericArgs)
				}
			}

			null -> Type.UNIT
		}
	}

	private fun complexPathUntilHead(path: ComplexPath): Item? {
		var item: Item? = currentModule
		for (segment in path.tail) {
			if (segment.segment.type == Token.Type.NA) {
				item = namespace.rootModule
				continue
			}
			// use for enums later
			val generics = resolveGenericParams(
				if (item != null) {
					item = resolveTypeItemField(item, segment.segment)
					if (item != null) Pair(item, segment.segment) else null
				} else null, segment.generics
			)
		}
		return item
	}

	private fun resolveComplexPathExpr(path: ComplexPath): Pair<Item, List<Type>>? {
		var item = complexPathUntilHead(path)
		val generics = resolveGenericParams(
			if (item != null) {
				item = resolveValueItemField(item, path.head.segment)
				if (item != null) Pair(item, path.head.segment) else null
			} else null, path.head.generics
		)
		return item?.let { Pair(item, generics) }
	}

	private fun resolveComplexPathType(path: ComplexPath): Pair<Item, List<Type>>? {
		var item = complexPathUntilHead(path)
		val generics = resolveGenericParams(
			if (item != null) {
				item = resolveTypeItemField(item, path.head.segment)
				if (item != null) Pair(item, path.head.segment) else null
			} else null, path.head.generics
		)
		return item?.let { Pair(item, generics) }
	}

	private fun resolveValueItemField(item: Item, field: Token): Item? {
		when (item) {
			is Function, is Variable -> throw IllegalArgumentException("This will never happen")
			is Module -> {
				val inner = item.values.get(field.lexeme)

				inner ?: zinc.reportCompileError(
					"Value item '${field.lexeme}' does not exist in module '${item.name}'.",
					field.asRange()
				)

				return inner
			}

			is TypeAlias -> {
				zinc.reportCompileError(
					"Cannot use '::' on a type directly. Try using a qualified path. Ex: <${item.name} as Trait>",
					field.asRange()
				)
				return null
			}
		}
	}

	private fun resolveTypeItemField(item: Item, field: Token): Item? {
		when (item) {
			is Function, is Variable -> throw IllegalArgumentException("This will never happen")
			is Module -> {
				val inner = item.types.get(field.lexeme)

				inner ?: zinc.reportCompileError(
					"Type item '${field.lexeme}' does not exist in module '${item.name}'.",
					field.asRange()
				)

				return inner
			}

			is TypeAlias -> {
				zinc.reportCompileError(
					"Cannot use '::' on a type directly. Try using a qualified path. Ex: <${item.name} as Trait>",
					field.asRange()
				)
				return null
			}
		}
	}

	private fun resolveGenericParams(itemApplied: Pair<Item, Token>?, generics: GenericArgs?): List<Type> {
		val passedInGenerics = generics?.args?.size ?: 0
		if (itemApplied != null) {
			val (itemApplied, token) = itemApplied
			if (itemApplied.genericArity != passedInGenerics) {
				zinc.reportCompileError(
					"Item '${itemApplied.name}' expected '${itemApplied.genericArity}' generic argument(s) but '${passedInGenerics}' were provided",
					token..(generics?.close ?: token)
				)
			}
		}
		return generics?.args?.map { type -> resolveType(type) } ?: emptyList()
	}

}