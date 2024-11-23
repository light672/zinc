package com.light672.zinc.resolution

import com.light672.zinc.CompilerError
import com.light672.zinc.ScopeInfo
import com.light672.zinc.ScopeInfo.Branch
import com.light672.zinc.Zinc
import com.light672.zinc.ast.*
import com.light672.zinc.ir.IRExpr
import com.light672.zinc.ir.IRPattern
import com.light672.zinc.ir.IRStmt
import com.light672.zinc.ir.IRType
import com.light672.zinc.item.Implementation
import com.light672.zinc.item.TypeItem
import com.light672.zinc.item.ValueItem

internal class Resolver(private val zinc: Zinc.Runtime) {
	fun resolve(scope: ScopeInfo) {
		for (impl in scope.implementationItems) resolveImplementation(impl, scope)
		for (impl in scope.implementationItems) resolveImplementationInterior(impl, scope)
		for ((name, item) in scope.types) {
			resolveTypeItem(item, scope)
		}

		for ((name, item) in scope.values) {
			resolveValueItem(item, scope)
		}
	}

	// implementations
	private fun resolveImplementation(impl: Implementation, scope: ScopeInfo) {
		val type = resolveType(impl.type, scope, true)
		val inheritedInterface = impl.inheritedInterface?.let { resolveType(impl.inheritedInterface, scope, true) }
		impl.irType = type
		impl.irInheritedInterface = inheritedInterface

		if (inheritedInterface != null && (inheritedInterface !is IRType.Item || inheritedInterface.typeItem !is TypeItem.Interface)) {
			zinc.reportCompileError(CompilerError.inheritingNonInterface(impl.inheritedInterface))
		}

		if (inheritedInterface != null) {
			for ((_, function) in impl.functions) {
				val name = (function as ValueItem.Function).name
				scope.interfaceImpls.add(type, inheritedInterface, name, function)
				val existingFunction = scope.impls.get(type, name, false) as ValueItem.Function?
				if (existingFunction == null) {
					scope.impls.add(type, name, function)
				} else if (existingFunction.parentType == ValueItem.Function.ParentType.INHERIT_IMPL) {
					scope.impls.add(type, name, ValueItem.Ambiguous)
				}
			}
		} else if (type !== IRType.Error) {
			for ((name, function) in impl.functions) {
				scope.impls.add(type, (function as ValueItem.Function).name, function)
			}
		}
	}

	private fun resolveImplementationInterior(impl: Implementation, scope: ScopeInfo) {
		for ((name, value) in impl.functions) {
			resolveValueItem(value, scope)
		}
	}

	// type items
	private fun resolveTypeItem(item: TypeItem, scope: ScopeInfo) {
		when (item) {
			is TypeItem.Module -> resolve(item.scope)
			is TypeItem.Struct -> resolveStruct(item, scope)
			is TypeItem.Interface -> resolveInterface(item, scope)
		}
	}

	private fun resolveStruct(struct: TypeItem.Struct, scope: ScopeInfo) {
		struct.irFields = struct.fields.associate { (token, type) -> Pair(token.lexeme, resolveType(type, scope)) }
	}

	private fun resolveInterface(int: TypeItem.Interface, scope: ScopeInfo) {
		for ((name, value) in int.functions) {
			val function = value as ValueItem.Function
			resolveFunction(function, scope)
		}
	}

	// value items

	private fun resolveValueItem(item: ValueItem, scope: ScopeInfo) {
		when (item) {
			is ValueItem.Function -> resolveFunction(item, scope)
			is ValueItem.UnitStruct -> {}
			is ValueItem.Variable -> throw IllegalArgumentException("should not show up")
			ValueItem.Ambiguous -> {}
		}
	}

	private fun resolveFunction(function: ValueItem.Function, scope: ScopeInfo) {
		function.irParameters = function.parameters.map { (pattern, type) ->
			Pair(resolvePattern(pattern, scope), resolveType(type, scope))
		}
		function.irReturnType = resolveType(function.returnType, scope)
		function.irBlock = resolveBlock(function.block, scope)
	}

	// types

	private fun resolveType(type: Type?, scope: ScopeInfo, inImpl: Boolean = false): IRType {
		return simplifyType(
			when (type) {
				is ComplexPath -> IRType.Item(resolveTypePath(type, scope, inImpl) ?: return IRType.Error)
				is Type.Tuple -> IRType.Tuple(type.types.map { type -> resolveType(type, scope, inImpl) })
				null -> IRType.Unit
			}
		)
	}

	private fun typeFromTypeItem(item: TypeItem): IRType {
		return simplifyType(
			when (item) {
				is TypeItem.Interface, is TypeItem.Struct -> IRType.Item(item)
				is TypeItem.Module -> throw IllegalArgumentException()
			}
		)
	}

	private fun simplifyType(type: IRType): IRType {
		return when (type) {
			IRType.Error, IRType.Unit -> type
			is IRType.Function -> IRType.Function(type.paramTypes.map { param -> simplifyType(param) }, simplifyType(type.returnType))
			is IRType.Item -> {
				when (type.typeItem) {
					is TypeItem.Interface, is TypeItem.Struct -> type
					is TypeItem.Module -> throw IllegalArgumentException("Modules should not be types")
				}
			}

			is IRType.Tuple -> IRType.Tuple(type.types.map { field -> simplifyType(field) })
		}
	}

	// statements
	private fun resolveExpressionStatement(
		exprStmt: Stmt.Expression,
		scope: ScopeInfo
	): IRStmt.Expression {
		return IRStmt.Expression(resolveExpression(exprStmt.expr, scope), exprStmt.trailing)
	}

	private fun resolveLetStatement(
		let: Stmt.Let,
		scope: ScopeInfo
	): IRStmt.Let {
		val irPattern = resolvePattern(let.pattern, scope)
		val type = let.type?.let { type -> resolveType(type, scope) }
		val initializer = let.initializer?.let { expr -> resolveExpression(expr, scope) }
		return IRStmt.Let(scope.values, irPattern, type, initializer)
	}

	// expressions
	private fun resolveExpression(expr: Expr, scope: ScopeInfo): IRExpr {
		return when (expr) {
			is Expr.Binary -> IRExpr.Binary(
				resolveExpression(expr.left, scope),
				expr.operator.type,
				resolveExpression(expr.right, scope)
			)

			is Expr.Block -> resolveBlock(expr, scope)
			is Expr.Call -> resolveCall(expr, scope)
			is Expr.Literal -> resolveLiteral(expr)
			is Expr.Tuple -> IRExpr.Tuple(expr.fields.map { expr -> resolveExpression(expr, scope) })
			is Expr.Unary -> IRExpr.Unary(expr.operator.type, resolveExpression(expr.right, scope))
			is Expr.Variable -> resolveValuePath(expr.path, scope)?.let { item -> IRExpr.Variable(item) } ?: IRExpr.Error
		}
	}

	private fun resolveBlock(
		block: Expr.Block,
		parentScope: ScopeInfo
	): IRExpr.Block {
		var values = block.scope.values
		val types = block.scope.types
		val impls = block.scope.impls
		val interfaceImpls = block.scope.interfaceImpls


		types.bindParent(parentScope.types)
		values.bindParent(parentScope.values)
		impls.bindParent(parentScope.impls)
		interfaceImpls.bindParent(parentScope.interfaceImpls)

		resolve(ScopeInfo(types, values, impls, interfaceImpls))

		return IRExpr.Block(block, block.stmts.map { stmt ->
			when (stmt) {
				is Stmt.Expression -> resolveExpressionStatement(stmt, ScopeInfo(types, values, impls, interfaceImpls))
				is Stmt.Let -> {
					val newBranch = Branch<ValueItem>(zinc)
					newBranch.bindParent(values)
					values = newBranch
					resolveLetStatement(stmt, ScopeInfo(types, values, impls, interfaceImpls))
				}
			}
		})
	}

	private fun resolveCall(expr: Expr.Call, scope: ScopeInfo): IRExpr.Call {
		return IRExpr.Call(
			resolveExpression(expr.callee, scope),
			expr.arguments.map { expr -> resolveExpression(expr, scope) }
		)
	}

	private fun resolveLiteral(expr: Expr.Literal): IRExpr.Literal {
		return IRExpr.Literal(
			when (expr.token.type) {
				TokenType.NUMBER -> java.lang.Double.parseDouble(expr.token.lexeme.toString())
				TokenType.STRING -> expr.token.lexeme
				else -> throw IllegalArgumentException()
			}
		)
	}

	// patterns
	private fun resolvePattern(
		pattern: Pattern,
		scope: ScopeInfo
	): IRPattern {
		return when (pattern) {
			is Pattern.Identifier -> {
				val variable = ValueItem.Variable(pattern.mut != null, pattern.identifier)
				scope.values.add(pattern.identifier, variable)
				IRPattern.Identifier(variable)
			}

			is Pattern.Tuple -> IRPattern.Tuple(pattern.fields.map { field -> resolvePattern(field, scope) })
			is Pattern.TypePath ->
				resolveValuePath(pattern.path, scope)?.let { valueItem -> IRPattern.Path(valueItem) } ?: IRPattern.Error

			is Pattern.Underscore -> IRPattern.Underscore

			is Pattern.Struct -> TODO()
			is Pattern.TupleStruct -> TODO()
		}
	}

	// paths

	private fun resolveValuePath(
		complexPath: ComplexPath,
		scope: ScopeInfo
	): ValueItem? {
		when (complexPath) {
			is ComplexPath.Normal -> {
				var currentTypes = scope.types
				var currentValues = scope.values
				for (segment in complexPath.body) {
					if (complexPath.body.last() === segment) {
						val valueItem = currentValues.get(segment.token)
						return valueItem
					}
					val typeItem = currentTypes.get(segment.token) ?: return null
					val (types, values) = namespacesFromTypeItem(typeItem, scope)
					currentTypes = types
					currentValues = values
				}
			}

			is ComplexPath.Qualified -> TODO("Implement qualified paths")
		}

		throw IllegalArgumentException("should have returned by now")
	}

	private fun resolveTypePath(complexPath: ComplexPath, scope: ScopeInfo, inImpl: Boolean = false): TypeItem? {
		when (complexPath) {
			is ComplexPath.Normal -> {
				var currentTypes = scope.types
				for (segment in complexPath.body) {
					val typeItem = currentTypes.get(segment.token) ?: return null
					if (complexPath.body.last() === segment) return typeItem
					if (inImpl && typeItem !is TypeItem.Module) {
						zinc.reportCompileError(CompilerError.indirectTypeInImpl(complexPath))
						return null
					}
					val (types, values) = namespacesFromTypeItem(typeItem, scope)
					currentTypes = types
				}
			}

			is ComplexPath.Qualified -> TODO("Implement qualified paths") // do something here with inImpl to throw an error if it is true
		}

		throw IllegalArgumentException("should have returned by now")
	}

	// utility

	private fun namespacesFromTypeItem(typeItem: TypeItem, scope: ScopeInfo): Pair<Branch<TypeItem>, Branch<ValueItem>> {
		return when (typeItem) {
			is TypeItem.Module -> Pair(typeItem.scope.types, typeItem.scope.values)
			is TypeItem.Struct -> {
				val branch = scope.impls.getImplementation(typeFromTypeItem(typeItem)) ?: Branch(zinc)
				Pair(Branch(zinc), branch)
			}

			is TypeItem.Interface -> Pair(Branch(zinc), typeItem.functions)
		}
	}
}