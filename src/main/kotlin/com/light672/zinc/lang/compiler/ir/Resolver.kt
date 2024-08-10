package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.Zinc
import com.light672.zinc.lang.compiler.ast.syntax.*
import com.light672.zinc.lang.tool.Either

internal class Resolver(private val namespace: Namespace, private val zinc: Zinc.Runtime) {
	private var currentModule: Module = namespace.rootModule
	fun resolveAndLower() {
		// TODO: find a way to better resolve global variables while keeping 'let' pattern semantics
		for (type in namespace.types) resolve(type)
		for (value in namespace.values) resolve(value)
	}


	private fun resolve(item: Item) {
		when (item) {
			is Function -> resolveFunction(item)
			is Module -> resolveModule(item)
			is TypeAlias -> TODO()
			is Variable -> {}
		}
	}

	private fun resolveModule(module: Module) {
		namespace.newTypes(module.types) {
			namespace.newValues(module.values) {
				resolveAndLower()
			}
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
				function.block = Either.Left(Unit)
			}

			is Either.Right -> {
				val block = function.declaration.scOrBlock
				function.block = Either.Right(lowerASTExpr(block.value) as IRExpr.Block)
			}
		}
	}

	private fun lowerBlock(block: Expr.Block, branch: Namespace.Branch): IRExpr.Block {
		val statements = ArrayList<IRStmt>()
		namespace.newValues(branch) {
			ArrayList<IRStmt>(block.stmts.size)
			for (stmt in block.stmts)
				lowerASTStmt(stmt)?.let { statements.add(it) }
		}
		return IRExpr.Block(branch, statements)
	}

	private fun lowerLet(stmt: Stmt.Variable, branch: Namespace.Branch): IRStmt.LetBinding {
		// TODO: handle variable initializers
		val irPattern = resolvePattern(stmt.pattern)
		namespace.newValues(branch) {
			namespace.addPatternLocals(stmt.pattern, irPattern)
		}
		return IRStmt.LetBinding(branch, irPattern)
	}

	private fun resolvePattern(pattern: Pattern): IRPattern {
		return when (pattern) {
			is Pattern.IdentifierPattern -> IRPattern.Identifier(Variable(pattern.name.lexeme, pattern.mut != null))
			is Pattern.PathPattern -> TODO()
		}
	}

	private fun lowerASTStmt(stmt: Stmt): IRStmt? {
		return when (stmt) {
			is Stmt.Module, is Stmt.Function, is Stmt.TypeAlias -> null
			is Stmt.Expression -> IRStmt.Expr(lowerASTExpr(stmt.expr), stmt.trailing)
			is Stmt.Variable -> lowerLet(stmt, namespace.values)
		}
	}

	private fun lowerASTExpr(expr: Expr): IRExpr {
		return when (expr) {
			is Expr.Binary -> {
				val a = lowerASTExpr(expr.a)
				val b = lowerASTExpr(expr.b)
				IRExpr.Binary(
					a, b, when (expr.operator.type) {
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
			}

			is Expr.Block -> lowerBlock(expr, Namespace.Branch(namespace.values, true))

			is Expr.Group -> lowerASTExpr(expr.expr)
			is Expr.Literal -> IRExpr.Literal(expr.literal)

			is Expr.Path -> resolveComplexPathExpr(expr.path)?.let {
				when (it.first) {
					is Function -> IRExpr.Function(it.first as Function, it.second)
					is Variable -> IRExpr.Variable(it.first as Variable)
					is Module, is TypeAlias -> throw IllegalArgumentException()
				}
			} ?: IRExpr.Variable.ERROR

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
		if (path.tail.isEmpty()) {
			val item = namespace.values.recursiveGet(path.head.segment.lexeme)
			if (item == null) zinc.reportCompileError("'${path.head.segment.lexeme}' does not exist in the current scope.", path.range())
			val generics = resolveGenericParams(item?.let { Pair(it, path.head.segment) }, path.head.generics)
			return (item?.let { Pair(it, generics) })
		}

		var item = complexPathUntilHead(path)
		val generics = resolveGenericParams(
			if (item != null) {
				item = resolveValueItemField(item, path.head.segment)
				item?.let { Pair(item, path.head.segment) }
			} else null, path.head.generics
		)
		return item?.let { Pair(item, generics) }
	}

	private fun resolveComplexPathType(path: ComplexPath): Pair<Item, List<Type>>? {
		if (path.tail.isEmpty()) {
			val item = namespace.types.recursiveGet(path.head.segment.lexeme)
			if (item == null) zinc.reportCompileError("'${path.head.segment.lexeme}' does not exist in the current scope.", path.range())
			val generics = resolveGenericParams(item?.let { Pair(it, path.head.segment) }, path.head.generics)
			return (item?.let { Pair(it, generics) })
		}

		var item = complexPathUntilHead(path)
		val generics = resolveGenericParams(
			if (item != null) {
				item = resolveTypeItemField(item, path.head.segment)
				item?.let { Pair(item, path.head.segment) }
			} else null, path.head.generics
		)
		return item?.let { Pair(item, generics) }
	}

	private fun resolveValueItemField(item: Item, field: Token): Item? {
		return when (item) {
			is Function, is Variable -> throw IllegalArgumentException("This will never happen")
			is Module -> {
				val inner = item.values.get(field.lexeme)

				inner ?: zinc.reportCompileError(
					"'${field.lexeme}' does not exist in module '${item.name}'.",
					field.asRange()
				)

				inner
			}

			is TypeAlias -> {
				zinc.reportCompileError(
					"Cannot use '::' on a type directly. Try using a qualified path. Ex: <${item.name} as Trait>",
					field.asRange()
				)
				null
			}
		}
	}

	private fun resolveTypeItemField(item: Item, field: Token): Item? {
		return when (item) {
			is Function, is Variable -> throw IllegalArgumentException("This will never happen")
			is Module -> {
				val inner = item.types.get(field.lexeme)

				inner ?: zinc.reportCompileError(
					"Type item '${field.lexeme}' does not exist in module '${item.name}'.",
					field.asRange()
				)

				inner
			}

			is TypeAlias -> {
				zinc.reportCompileError(
					"Cannot use '::' on a type directly. Try using a qualified path. Ex: <${item.name} as Trait>",
					field.asRange()
				)
				null
			}
		}
	}

	private fun resolveGenericParams(itemApplied: Pair<Item, Token>?, generics: GenericArgs?): List<Type> {
		val passedInGenerics = generics?.args?.size ?: 0
		itemApplied?.let { (itemApplied, token) ->
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