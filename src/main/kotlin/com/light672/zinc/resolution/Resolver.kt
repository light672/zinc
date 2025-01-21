package com.light672.zinc.resolution

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.ComplexPath
import com.light672.zinc.ast.ComplexSegment
import com.light672.zinc.ast.Token
import kotlin.math.max
import com.light672.zinc.ast.Expr as ASTExpr
import com.light672.zinc.ast.FunctionParam as ASTFunctionParam
import com.light672.zinc.ast.GenericArgs as ASTGenericArgs
import com.light672.zinc.ast.GenericParams as ASTGenericParams
import com.light672.zinc.ast.Pattern as ASTPattern
import com.light672.zinc.ast.Stmt as ASTStmt
import com.light672.zinc.ast.Type as ASTType


internal class Resolver(val zinc: Zinc.Runtime) {

	fun defineScope(stmt: ASTStmt, scope: Scope) {
		when (stmt) {
			is ASTStmt.Expression, is ASTStmt.Let -> {}
			is ASTStmt.Function -> {
				val item = ValueItem.Function(stmt)
				stmt.item = item
				stmt.genericScope = scope.newItem()
				addToScope(stmt.name.lexeme!!, stmt.range(), item, scope, true)
			}

			is ASTStmt.FunctionNoBlock -> {
				val item = ValueItem.Function(stmt)
				stmt.item = item
				stmt.genericScope = scope.newItem()
				addToScope(stmt.name.lexeme!!, stmt.range(), item, scope, true)
			}

			is ASTStmt.Module -> {
				val moduleScope = Scope()
				val module = TypeItem.Module(scope, stmt)
				stmt.statements.forEach { defineScope(it, moduleScope) }
				stmt.item = module
				addToScope(stmt.name.lexeme!!, stmt.range(), module, scope, true)
			}

			is ASTStmt.Struct -> {
				val item = TypeItem.Struct(stmt.fields.associate { (token, type) -> Pair(token.lexeme!!, type) }, stmt)
				stmt.item = item
				stmt.genericScope = scope.newItem()
				addToScope(stmt.name.lexeme!!, stmt.range(), item, scope, true)
			}

			is ASTStmt.TupleStruct -> {
				val item = TypeItem.TupleStruct(stmt)
				stmt.item = item
				stmt.genericScope = scope.newItem()
				addToScope(stmt.name.lexeme!!, stmt.range(), item, scope, true)
			}

			is ASTStmt.UnitStruct -> {
				val item = TypeItem.UnitStruct(stmt)
				stmt.item = item
				stmt.genericScope = scope.newItem()
				addToScope(stmt.name.lexeme!!, stmt.range(), item, scope, true)
			}
		}
	}

	fun addToScope(name: CharSequence, declRange: Token.Range, item: TypeItem, scope: Scope, shadowable: Boolean) =
		addToBranch(name, declRange, item, scope.types, TypeItem.Ambiguous, shadowable)

	fun addToScope(name: CharSequence, declRange: Token.Range, item: ValueItem, scope: Scope, shadowable: Boolean) =
		addToBranch(name, declRange, item, scope.values, ValueItem.Ambiguous, shadowable)


	private fun <T> addToBranch(name: CharSequence, declRange: Token.Range, item: T, branch: Scope.Branch<T>, ambigiousItem: T, shadowable: Boolean) {
		val existing = if (shadowable) branch.data[name] else getFromBranch(name, branch)
		if (existing != null) {
			zinc.reportCompileError(
				if (shadowable)
					CompilerError.nameAlreadyExists(name, declRange, "scope")
				else
					CompilerError.cannotShadowName(name, declRange)
			)
			branch.data[name] = ambigiousItem
		} else branch.data[name] = item
	}

	// statements
	fun stmt(stmt: ASTStmt, scope: Scope): Stmt {
		return when (stmt) {
			is ASTStmt.Expression -> Stmt.Expression(expr(stmt.expr, scope), stmt)
			is ASTStmt.Function -> function(stmt, scope)
			is ASTStmt.FunctionNoBlock -> functionNoBlock(stmt, scope)
			is ASTStmt.Let -> let(stmt, scope)
			is ASTStmt.Module -> module(stmt)
			is ASTStmt.Struct -> struct(stmt, scope)
			is ASTStmt.TupleStruct -> tupleStruct(stmt, scope)
			is ASTStmt.UnitStruct -> unitStruct(stmt, scope)
		}
	}

	private fun function(stmt: ASTStmt.Function, scope: Scope): Stmt {
		stmt.genericScope.values.parent = scope.values
		stmt.genericScope.types.parent = scope.types
		val genericParams = genericParams(stmt.genericParams, stmt.genericScope)
		val params = stmt.params.map { param -> functionParam(param, stmt.genericScope) }
		val returnType = stmt.returnType?.let { type(it, stmt.genericScope) }
		val block = block(stmt.block, stmt.genericScope)
		return Stmt.Function(genericParams, params, returnType, block, stmt)
	}

	private fun functionNoBlock(stmt: ASTStmt.FunctionNoBlock, scope: Scope): Stmt {
		stmt.genericScope.values.parent = scope.values
		stmt.genericScope.types.parent = scope.types
		val genericParams = genericParams(stmt.genericParams, stmt.genericScope)
		val params = stmt.params.map { param -> functionParam(param, stmt.genericScope) }
		val returnType = stmt.returnType?.let { type(it, stmt.genericScope) }
		return Stmt.FunctionNoBlock(genericParams, params, returnType, stmt)
	}

	private fun functionParam(param: ASTFunctionParam, scope: Scope): FunctionParam {
		return when (param) {
			is ASTFunctionParam.PatternParam -> FunctionParam.PatternParam(pattern(param.pattern, scope), type(param.type, scope))
			is ASTFunctionParam.SelfParam -> TODO("add this eventually")
		}
	}

	private fun let(stmt: ASTStmt.Let, scope: Scope): Stmt {
		val pattern = pattern(stmt.pattern, scope)
		val type = stmt.type?.let { type(it, scope) }
		val initializer = stmt.initializer?.let { expr(it, scope) }
		return Stmt.Let(pattern, type, initializer)
	}

	private fun module(stmt: ASTStmt.Module): Stmt {
		val moduleScope = stmt.item.scope
		return Stmt.Module(stmt.statements.map { stmt(it, moduleScope) }, stmt)
	}

	private fun struct(stmt: ASTStmt.Struct, scope: Scope): Stmt {
		stmt.genericScope.values.parent = scope.values
		stmt.genericScope.types.parent = scope.types
		val genericParams = genericParams(stmt.genericParams, stmt.genericScope)
		val fields = stmt.fields.associate { (token, type) -> Pair(token.lexeme!!, type(type, stmt.genericScope)) }
		return Stmt.Struct(genericParams, fields, stmt)
	}

	private fun tupleStruct(stmt: ASTStmt.TupleStruct, scope: Scope): Stmt {
		stmt.genericScope.values.parent = scope.values
		stmt.genericScope.types.parent = scope.types
		val genericParams = genericParams(stmt.genericParams, stmt.genericScope)
		val fields = stmt.fields.map { type -> type(type, stmt.genericScope) }
		return Stmt.TupleStruct(genericParams, fields, stmt)
	}

	private fun unitStruct(stmt: ASTStmt.UnitStruct, scope: Scope): Stmt {
		stmt.genericScope.values.parent = scope.values
		stmt.genericScope.types.parent = scope.types
		val genericParams = genericParams(stmt.genericParams, stmt.genericScope)
		return Stmt.UnitStruct(genericParams, stmt)
	}


	// expressions

	private fun expr(expr: ASTExpr, scope: Scope): Expr {
		return when (expr) {
			is ASTExpr.Binary ->
				Expr.Binary(expr(expr.left, scope), expr.operator, expr(expr.right, scope), expr)

			is ASTExpr.Block ->
				block(expr, scope)

			is ASTExpr.Break ->
				Expr.Break(expr.expr?.let { expr(it, scope) }, expr)

			is ASTExpr.Call ->
				Expr.Call(expr(expr.callee, scope), expr.args.map { expr(it, scope) }, expr)

			is ASTExpr.Closure ->
				closure(expr, scope)

			is ASTExpr.FieldGet ->
				fieldGet(expr, scope)

			is ASTExpr.For ->
				forExpr(expr, scope)

			is ASTExpr.Group ->
				group(expr, scope)

			is ASTExpr.If ->
				Expr.If(expr(expr.condition, scope), block(expr.thenBlock, scope), expr.elseExpr?.let { expr(it, scope) }, expr)

			is ASTExpr.Index ->
				Expr.Index(expr(expr.callee, scope), expr.args.map { expr(it, scope) }, expr)

			is ASTExpr.Literal ->
				Expr.Literal(expr)

			is ASTExpr.Loop ->
				Expr.Loop(block(expr.block, scope), expr)

			is ASTExpr.Match ->
				match(expr, scope)

			is ASTExpr.Path ->
				pathExpr(expr, scope)

			is ASTExpr.Range ->
				// change inclusive to check if token is ..= once that token is lexed
				Expr.Range(expr.left?.let { expr(it, scope) }, expr.right?.let { expr(it, scope) }, false, expr)

			is ASTExpr.Return ->
				Expr.Return(expr.expr?.let { expr(it, scope) }, expr)

			is ASTExpr.Unary ->
				Expr.Unary(expr.operator, expr(expr.right, scope), expr)

			is ASTExpr.While ->
				Expr.While(expr(expr.condition, scope), block(expr.block, scope), expr)
		}
	}

	private fun block(block: ASTExpr.Block, scope: Scope): Expr.Block {
		var scope = scope.newScope()
		block.stmts.forEach { defineScope(it, scope) }
		val irStmts = block.stmts.map { stmt ->
			if (stmt is ASTStmt.Let) scope = scope.newValues()
			stmt(stmt, scope)
		} // need to define function scopes inside of functions

		return Expr.Block(irStmts, block)
	}

	private fun closure(closure: ASTExpr.Closure, scope: Scope): Expr.Closure {
		val paramScope = scope.newScope()
		val params = closure.params.map { (pattern, type) -> Pair(pattern(pattern, paramScope).affirmIrrefutable(), type?.let { type(type, scope) }) }
		val expr = expr(closure.expr, paramScope)
		return Expr.Closure(params, expr, closure)
	}

	private fun fieldGet(fieldGet: ASTExpr.FieldGet, scope: Scope): Expr.FieldGet {
		val callee = expr(fieldGet.callee, scope)
		val generics = fieldGet.segment.generics?.let { genericArgs(it, scope) }

		return Expr.FieldGet(callee, Pair(fieldGet.segment.id, generics), fieldGet)
	}

	private fun forExpr(forExpr: ASTExpr.For, scope: Scope): Expr.For {
		val paramScope = scope.newScope()
		val pattern = pattern(forExpr.pattern, paramScope).affirmIrrefutable()
		val iterator = expr(forExpr.iterator, scope)
		val block = block(forExpr.block, scope)
		return Expr.For(pattern, iterator, block, forExpr)
	}

	private fun group(expr: ASTExpr.Group, scope: Scope): Expr {
		val exprs = expr.expressions.map { expr(it, scope) }
		return if (exprs.size == 1) exprs[0] // this does not include the parenthesis in the expression range for error reporting TODO: maybe fix in future
		else Expr.Tuple(exprs, expr)
	}

	private fun match(matchExpr: ASTExpr.Match, scope: Scope): Expr.Match {
		val expr = expr(matchExpr.expr, scope)
		val branches = matchExpr.branches.map { (pattern, expr) ->
			val paramScope = scope.newScope()
			val pattern = pattern(pattern, paramScope)
			val expr = expr(expr, paramScope)
			Pair(pattern, expr)
		}
		return Expr.Match(expr, branches, matchExpr)
	}

	private fun pathExpr(expr: ASTExpr.Path, scope: Scope): Expr.Item {
		return Expr.Item(valuePath(expr.path, scope), expr)
	}

	// types
	private fun type(type: ASTType, scope: Scope): Type {
		return when (type) {
			is ASTType.Error -> Type.Error(type.range)
			is ASTType.Path -> typePath(type.path, scope)?.let { Type.Item(it, type) } ?: Type.Error(type.range())
			is ASTType.Tuple -> Type.Tuple(type.fields.map { type(it, scope) }, type)
		}
	}

	// patterns
	private fun pattern(pattern: ASTPattern, scope: Scope): Pattern {
		return when (pattern) {
			is ASTPattern.Identifier -> {
				val variable = ValueItem.Variable(false, pattern)
				val newPattern = Pattern.Variable(variable, pattern)
				addToScope(pattern.token.lexeme!!, pattern.token.asRange(), variable, scope, true)
				newPattern
			}

			is ASTPattern.Literal -> Pattern.Literal(pattern.token)
			is ASTPattern.Tuple -> Pattern.Tuple(pattern.fields.map { ast -> pattern(ast, scope) }, pattern)
			is ASTPattern.Wildcard -> Pattern.WildCard(pattern.token)
		}
	}

	private fun Pattern.affirmIrrefutable(): Pattern {
		if (isRefutable(this))
			zinc.reportCompileError(CompilerError.patternMustBeIrrefutable(range()))
		return this
	}

	private fun isRefutable(pattern: Pattern): Boolean {
		return when (pattern) {
			is Pattern.Literal -> true
			is Pattern.Tuple -> pattern.fields.any { isRefutable(it) }
			is Pattern.Variable -> false
			is Pattern.WildCard -> false
		}
	}

	// generics

	private fun genericArgs(generics: ASTGenericArgs, scope: Scope): GenericArgs {
		return GenericArgs(generics.types.map { type(it, scope) }, generics)
	}

	private fun genericParams(genericParams: ASTGenericParams?, genericScope: Scope): GenericParams? {
		genericParams ?: return null
		val parameters = genericParams.params.map { ident -> declareGeneric(ident, genericScope) }
		return GenericParams(parameters, genericParams)
	}

	private fun declareGeneric(name: Token, scope: Scope): TypeItem.Generic {
		val genericItem = TypeItem.Generic(name)
		addToScope(name.lexeme!!, name.asRange(), genericItem, scope, false)
		return genericItem
	}

	// paths
	private fun getTypeInItem(item: TypeItemReference, name: Token, generics: GenericArgs?): TypeItemReference? {
		val (item, _generics) = unwrapItemFromTypeRef(item)
		return when (item) {
			is TypeItem.Module -> {
				val ref = item.scope.types.data[name.lexeme!!]?.let { TypeItemReference.Simple(it, generics) }
				ref ?: zinc.reportCompileError(CompilerError.nameNotFound(name.lexeme, name.asRange(), "module"))
				ref
			}


			is TypeItem.Struct, is TypeItem.TupleStruct, is TypeItem.UnitStruct, is TypeItem.Generic, is TypeItem.Primitive -> {
				zinc.reportCompileError(CompilerError.useQualifiedPath(name, name.asRange()))
				null
			}

			TypeItem.Ambiguous -> null
		}
	}

	private fun getValueInItem(item: TypeItemReference, name: Token, generics: GenericArgs?): ValueItemReference? {
		val (item, _generics) = unwrapItemFromTypeRef(item)
		return when (item) {
			TypeItem.Ambiguous -> null
			is TypeItem.Module -> {
				val ref = item.scope.values.data[name.lexeme!!]?.let { ValueItemReference.Simple(it, generics) }
				ref ?: zinc.reportCompileError(CompilerError.nameNotFound(name.lexeme, name.asRange(), "module"))
				ref
			}

			is TypeItem.Struct, is TypeItem.TupleStruct, is TypeItem.UnitStruct, is TypeItem.Generic, is TypeItem.Primitive -> {
				zinc.reportCompileError(CompilerError.useQualifiedPath(name, name.asRange()))
				null
			}
		}
	}

	private fun verifyUseOfGenerics(segment: ComplexSegment, valueRef: ValueItemReference) {
		if (valueRef is ValueItemReference.TypeAssociated) return
		if (valueRef is ValueItemReference.Simple) {
			when (valueRef.item) {
				is ValueItem.Variable ->
					zinc.reportCompileError(CompilerError.genericsNotAllowedIn(segment.range(), "variable"))

				else -> {}
			}
		}
	}

	private fun verifyUseOfGenerics(segment: ComplexSegment, itemRef: TypeItemReference) {
		val (item, generics) = unwrapItemFromTypeRef(itemRef)
		if (generics != null)
			when (item) {
				is TypeItem.Module ->
					zinc.reportCompileError(CompilerError.genericsNotAllowedIn(segment.range(), "module"))

				else -> {}
			}
	}


	private fun unwrapItemFromTypeRef(typeRef: TypeItemReference): Pair<TypeItem, GenericArgs?> {
		return when (typeRef) {
			is TypeItemReference.Simple -> Pair(typeRef.item, typeRef.generics)
		}
	}


	private fun body(path: ComplexPath.Normal, scope: Scope, index: Int = path.body.size - 1): TypeItemReference? {
		val segment = path.body[index]
		val generics = segment.generics?.let { genericArgs(it, scope) }

		if (index == 0)
			return getType(segment.id, scope)?.let { TypeItemReference.Simple(it, generics) }

		val previous = body(path, scope, index - 1) ?: return null
		return getTypeInItem(previous, segment.id, generics).also { verifyUseOfGenerics(segment, previous) }
	}


	private fun typePath(path: ComplexPath, scope: Scope): TypeItemReference? {
		return when (path) {
			is ComplexPath.Error -> null
			is ComplexPath.Normal -> typePath(path, scope)
			is ComplexPath.Qualified -> typePath(path, scope)
		}
	}


	private fun typePath(path: ComplexPath.Normal, scope: Scope): TypeItemReference? {
		return body(path, scope)
	}


	private fun typePath(path: ComplexPath.Qualified, scope: Scope): TypeItemReference? {
		val type = type(path.type, scope)
		val trait = path.trait?.let { typePath(it, scope) }
		if (trait == null) {
			zinc.reportCompileError(CompilerError.useQualifiedPath(path.body.first().id, path.range()))
			return null
		}

		val (traitItem, _traitGenerics) = unwrapItemFromTypeRef(trait)
		TODO("traits not yet implemented")
	}

	private fun valuePath(path: ComplexPath, scope: Scope): ValueItemReference? {
		return when (path) {
			is ComplexPath.Error -> null
			is ComplexPath.Normal -> valuePath(path, scope)
			is ComplexPath.Qualified -> valuePath(path, scope)
		}
	}

	private fun valuePath(path: ComplexPath.Normal, scope: Scope): ValueItemReference? {
		val finalSegment = path.body.last()
		val generics = finalSegment.generics?.let { genericArgs(it, scope) }
		if (path.body.size == 1) return getValue(finalSegment.id, scope)?.let { ValueItemReference.Simple(it, generics) }
		val untilEnd = body(path, scope, max(0, path.body.size - 2)) ?: return null
		return getValueInItem(untilEnd, finalSegment.id, generics)?.also { verifyUseOfGenerics(finalSegment, it) }
	}

	private fun valuePath(path: ComplexPath.Qualified, scope: Scope): ValueItemReference? {
		val type = type(path.type, scope)
		val trait = path.trait?.let { typePath(it, scope) }
		if (trait == null) {
			zinc.reportCompileError(CompilerError.useQualifiedPath(path.body.first().id, path.range()))
			return null
		}

		val (traitItem, _traitGenerics) = unwrapItemFromTypeRef(trait)
		TODO("traits not yet implemented")
	}


	// scope

	private fun <T> getFromBranch(name: CharSequence, branch: Scope.Branch<T>, depth: Int = branch.depthSinceItem): Pair<T, Int>? {
		return branch.data[name]?.let { Pair(it, depth) } ?: branch.parent?.let { getFromBranch(name, it, depth - 1) }
	}

	private fun getValue(name: Token, scope: Scope, envName: String = "scope"): ValueItem? {
		val item = getFromBranch(name.lexeme!!, scope.values)
		if (item == null) {
			zinc.reportCompileError(CompilerError.nameNotFound(name.lexeme, name.asRange(), envName))
			return null
		}
		if (item.second < 0 && item.first is ValueItem.Variable) {
			zinc.reportCompileError(CompilerError.cannotCaptureDynamicEnvironment(name))
			return null
		}
		return item.first
	}

	private fun getType(name: Token, scope: Scope, envName: String = "scope"): TypeItem? {
		val item = getFromBranch(name.lexeme!!, scope.types)
		item ?: zinc.reportCompileError(CompilerError.nameNotFound(name.lexeme, name.asRange(), envName))
		return item?.first
	}


}