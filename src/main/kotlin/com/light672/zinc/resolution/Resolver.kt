package com.light672.zinc.resolution

import com.light672.zinc.CompilerError
import com.light672.zinc.Scope
import com.light672.zinc.Scope.Branch
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
	fun resolve(scope: Scope) {
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
	private fun resolveImplementation(impl: Implementation, scope: Scope) {
		val type = resolveType(impl.type, scope, true)
		val inheritedInterface = impl.inheritedInterface?.let { resolveType(impl.inheritedInterface, scope, true) }
		impl.irType = type
		impl.irInheritedInterface = inheritedInterface

		if (inheritedInterface != null && (inheritedInterface !is IRType.Item || inheritedInterface.typeItem !is TypeItem.Interface)) {
			zinc.reportCompileError(CompilerError.inheritingNonInterface(impl.inheritedInterface))
		}

		if (type === IRType.Error) return
		if (inheritedInterface == null) {
			for ((_, function) in impl.functions) {
				scope.impls.add(type, (function as ValueItem.Function).name, function)
			}
			return
		}

		val interfaceItem = (inheritedInterface as? IRType.Item)?.typeItem as? TypeItem.Interface
		val mutableFunctionMap = interfaceItem?.functions?.associateTo(HashMap()) { (name, value) -> Pair(name, value) }
		val originalFunctionMap = mutableFunctionMap?.clone() as Map<CharSequence, ValueItem>?

		for ((_, function) in impl.functions) {
			val name = (function as ValueItem.Function).name
			addFunctionToInheritImpl(type, inheritedInterface, name, function, originalFunctionMap, mutableFunctionMap, scope)
		}

		mutableFunctionMap?.let { map ->
			for ((name, value) in map) {
				value as ValueItem.Function
				addFunctionToInheritImpl(type, inheritedInterface, value.name, value, null, null, scope)
				if (value.block != null)
					map.remove(name)
			}
			if (map.isNotEmpty()) {
				zinc.reportCompileError(
					CompilerError.missingInterfaceMembers(
						map.map { it.key },
						impl.keyword.line..impl.inheritedInterface.last().line,
						impl.keyword.rangeOnLine.first..impl.inheritedInterface.last().rangeOnLine.last
					)
				)
			}
		}
	}

	private fun addFunctionToInheritImpl(
		type: IRType,
		inheritedInterface: IRType,
		name: Token,
		function: ValueItem.Function,
		originalFunctionMap: Map<CharSequence, ValueItem>?,
		mutableFunctionMap: HashMap<CharSequence, ValueItem>?,
		scope: Scope
	) {
		scope.interfaceImpls.add(type, inheritedInterface, name, function)

		mutableFunctionMap?.let { map ->
			if (!originalFunctionMap!!.containsKey(name.lexeme)) {
				zinc.reportCompileError(CompilerError.nameIsNotMemberOf(name, inheritedInterface))
			} else map.remove(name.lexeme)
		}

		val existingFunction = scope.impls.get(type, name, false) as ValueItem.Function?

		if (existingFunction == null) {
			scope.impls.add(type, name, function)
		} else if (existingFunction.parentType == ValueItem.Function.ParentType.INHERIT_IMPL) {
			scope.impls.add(type, name, ValueItem.Ambiguous)
		}
	}

	private fun resolveImplementationInterior(impl: Implementation, scope: Scope) {
		for ((name, value) in impl.functions) {
			resolveValueItem(value, scope)
		}
	}

	// type items
	private fun resolveTypeItem(item: TypeItem, scope: Scope) {
		when (item) {
			is TypeItem.Module -> resolve(item.scope)
			is TypeItem.Struct -> resolveStruct(item, scope)
			is TypeItem.Interface -> resolveInterface(item, scope)
		}
	}

	private fun resolveStruct(struct: TypeItem.Struct, scope: Scope) {
		struct.irFields = struct.fields.associate { (token, type) -> Pair(token.lexeme, resolveType(type, scope)) }
	}

	private fun resolveInterface(int: TypeItem.Interface, scope: Scope) {
		for ((name, value) in int.functions) {
			val function = value as ValueItem.Function
			resolveFunction(function, false, scope)
		}
	}

	// value items

	private fun resolveValueItem(item: ValueItem, scope: Scope) {
		when (item) {
			is ValueItem.Function -> resolveFunction(item, true, scope)
			is ValueItem.UnitStruct -> {}
			is ValueItem.Variable -> throw IllegalArgumentException("should not show up")
			ValueItem.Ambiguous -> {}
		}
	}

	private fun resolveFunction(function: ValueItem.Function, mustHaveBody: Boolean, scope: Scope) {
		function.irParameters = function.parameters.map { (pattern, type) ->
			Pair(resolvePattern(pattern, scope), resolveType(type, scope))
		}
		function.irReturnType = resolveType(function.returnType, scope)
		function.irBlock = function.block?.let { block -> resolveBlock(block, scope) }
		if (mustHaveBody && function.block == null)
			zinc.reportCompileError(CompilerError.functionMustHaveBody(function.name, function.semicolon!!))
	}

	// types

	private fun resolveType(type: Type?, scope: Scope, inImpl: Boolean = false): IRType {
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
		scope: Scope
	): IRStmt.Expression {
		return IRStmt.Expression(resolveExpression(exprStmt.expr, scope), exprStmt.trailing)
	}

	private fun resolveLetStatement(
		let: Stmt.Let,
		scope: Scope
	): IRStmt.Let {
		val irPattern = resolvePattern(let.pattern, scope)
		val type = let.type?.let { type -> resolveType(type, scope) }
		val initializer = let.initializer?.let { expr -> resolveExpression(expr, scope) }
		return IRStmt.Let(scope.values, irPattern, type, initializer)
	}

	// expressions
	private fun resolveExpression(expr: Expr, scope: Scope): IRExpr {
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
		parentScope: Scope
	): IRExpr.Block {
		var values = block.scope.values
		val types = block.scope.types
		val impls = block.scope.impls
		val interfaceImpls = block.scope.interfaceImpls
		val implementationItems = block.scope.implementationItems


		types.bindParent(parentScope.types)
		values.bindParent(parentScope.values)
		impls.bindParent(parentScope.impls)
		interfaceImpls.bindParent(parentScope.interfaceImpls)

		resolve(Scope(types, values, impls, interfaceImpls, implementationItems))

		return IRExpr.Block(block, block.stmts.map { stmt ->
			when (stmt) {
				is Stmt.Expression -> resolveExpressionStatement(stmt, Scope(types, values, impls, interfaceImpls, implementationItems))
				is Stmt.Let -> {
					val newBranch = Branch<ValueItem>(zinc)
					newBranch.bindParent(values)
					values = newBranch
					resolveLetStatement(stmt, Scope(types, values, impls, interfaceImpls, implementationItems))
				}
			}
		})
	}

	private fun resolveCall(expr: Expr.Call, scope: Scope): IRExpr.Call {
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
		scope: Scope
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
		scope: Scope
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

	private fun resolveTypePath(complexPath: ComplexPath, scope: Scope, inImpl: Boolean = false): TypeItem? {
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

	private fun namespacesFromTypeItem(typeItem: TypeItem, scope: Scope): Pair<Branch<TypeItem>, Branch<ValueItem>> {
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