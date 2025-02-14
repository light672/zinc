package com.light672.zinc.hir

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.ComplexPath
import com.light672.zinc.ast.ComplexSegment
import com.light672.zinc.ast.Token
import com.light672.zinc.dsr.*
import com.light672.zinc.hir.AssociatedStmt
import com.light672.zinc.hir.Expr
import com.light672.zinc.hir.FunctionParam
import com.light672.zinc.hir.Pattern
import com.light672.zinc.hir.Stmt
import com.light672.zinc.ast.GenericArgs as ASTGenericArgs
import com.light672.zinc.ast.Type as ASTType
import com.light672.zinc.ast.TypeParamBounds as ASTTypeParamBounds
import com.light672.zinc.ast.WhereClause as ASTWhereClause
import com.light672.zinc.dsr.AssociatedStmt as DSRAssociatedStmt
import com.light672.zinc.dsr.Expr as DSRExpr
import com.light672.zinc.dsr.FunctionParam as DSRFunctionParam
import com.light672.zinc.dsr.GenericParams as DSRGenericParams
import com.light672.zinc.dsr.Pattern as DSRPattern
import com.light672.zinc.dsr.Stmt as DSRStmt

private typealias Values = Resolver.BranchNode<ValueItem>
private typealias Types = Resolver.BranchNode<TypeItem>

internal class Resolver(val zinc: Zinc.Runtime) {
	data class BranchNode<T>(val branch: Branch<T>, val indexSinceDecl: Int, val next: BranchNode<T>?)

	/**
	 * @param stmt   The DSR statement being converted into HIR.
	 * @param values The value scope being used to resolve the statement.
	 * @param types  The type scope being used to resolve the statement.
	 * @return       The converted DSR statement as a HIR statement containing resolved names as well as the next value branch that should be used.
	 */
	fun stmt(stmt: DSRStmt, values: Values, types: Types): Pair<Stmt, Values> {
		return when (stmt) {
			is DSRStmt.Function       -> Pair(function(stmt, values, types, false), values)
			is DSRStmt.Expression     -> Pair(Stmt.Expression(expr(stmt.expr, values, types), stmt.ast), values)
			is DSRStmt.Implementation -> Pair(implementation(stmt, values, types), values)
			is DSRStmt.Let            -> let(stmt, values, types)
			is DSRStmt.Module         -> Pair(module(stmt), values)
			is DSRStmt.Trait          -> Pair(trait(stmt, values, types), values)
			is DSRStmt.Struct         -> Pair(struct(stmt, types), values)
			is DSRStmt.TupleStruct    -> Pair(tupleStruct(stmt, types), values)
			is DSRStmt.UnitStruct     -> Pair(unitStruct(stmt, types), values)
		}
	}

	/**
	 * @param stmt   The DSR associated statement being converted into HIR.
	 * @param values The value scope being used to resolve the statement.
	 * @param types  The type scope being used to resolve the statement.
	 * @return       The converted DSR associated statement as a HIR associated statement
	 */
	private fun associatedStmt(stmt: DSRAssociatedStmt, values: Values, types: Types): AssociatedStmt {
		return when (stmt) {
			is DSRStmt.Function -> function(stmt, values, types, true)
		}
	}

	/**
	 * @param stmt       The DSR function being converted into HIR.
	 * @param values     The value scope used to resolve the statement.
	 * @param types      The type scope being used to resolve the statement.
	 * @param associated If true, the generic parameter branch will be pushed with a depth of 1 rather than 0 to allow the use of the generic parameters
	 * 					 from implementations and traits.
	 * @return           The converted DSR function as a HIR function containing resolved names.
	 */
	private fun function(stmt: DSRStmt.Function, values: Values, types: Types, associated: Boolean): Stmt.Function {
		val indexSinceDecl = if (associated) 1 else 0
		val values = Values(stmt.parameterBranch, indexSinceDecl, values)
		val types = Types(stmt.genericParams?.branch ?: Branch(), indexSinceDecl, types)
		val whereClause = whereClause(stmt.genericParams, stmt.ast.whereClause, types)
		val params = stmt.parameters.map { param ->
			when (param) {
				is DSRFunctionParam.Pattern -> FunctionParam.Pattern(pattern(param.pattern, values, types), type(param.ast.type, types), param.ast)
				is DSRFunctionParam.Self    -> FunctionParam.Self(param, param.ast.type?.let { type -> type(type, types) })
			}
		}

		val block = stmt.block?.let { block -> block(block, values, types) }
		return Stmt.Function(stmt.genericParams, whereClause, params, block, stmt.ast)
	}


	/**
	 * @param stmt   The DSR implementation being converted into HIR.
	 * @param values The value scope used to resolve the statement.
	 * @param types  The type scope being used to resolve the statement.
	 * @return       The converted DSR implementation as a HIR implementation containing resolved names.
	 */
	private fun implementation(stmt: DSRStmt.Implementation, values: Values, types: Types): Stmt {
		val values = Values(Branch(), 0, values)
		val types = Types(stmt.genericParams?.branch ?: Branch(), 0, types)
		val whereClause = whereClause(stmt.genericParams, stmt.ast.whereClause, types)
		val type = type(stmt.ast.type, types)
		val statements = stmt.statements.map { stmt -> associatedStmt(stmt, values, types) }

		if (stmt.ast.trait == null) return Stmt.InherentImpl(stmt.genericParams, whereClause, type, statements, stmt.ast)
		val trait = traitItem(stmt.ast.trait, types)
		return Stmt.TraitImpl(stmt.genericParams, whereClause, type, trait, statements, stmt.ast)
	}

	/**
	 * @param stmt   The DSR trait being converted into HIR.
	 * @param values The value scope used to resolve the statement.
	 * @param types  The type scope being used to resolve the statement.
	 * @return       The converted DSR trait as a HIR trait containing resolved names.
	 */
	private fun trait(stmt: DSRStmt.Trait, values: Values, types: Types): Stmt.Trait {
		val values = Values(Branch(), 0, values)
		val types = Types(stmt.genericParams?.branch ?: Branch(), 0, types)
		val whereClause = whereClause(stmt.genericParams, stmt.ast.whereClause, types)
		val statements = stmt.statements.map { stmt -> associatedStmt(stmt, values, types) }
		return Stmt.Trait(stmt.genericParams, whereClause, statements, stmt.ast)
	}

	/**
	 * @param stmt   The DSR let statement being converted into HIR.
	 * @param values The value scope used to resolve the statement.
	 * @param types  The type scope being used to resolve the statement.
	 * @return       The converted DSR let statement as a HIR let statement containing resolved names as well as the next value branch that should be used.
	 */
	private fun let(stmt: DSRStmt.Let, values: Values, types: Types): Pair<Stmt, Values> {
		val newValues = Values(stmt.branch, values.indexSinceDecl + 1, values)
		val pattern = pattern(stmt.pattern, values, types)
		val type = stmt.ast.type?.let { type -> type(type, types) }
		val initializer = stmt.initializer?.let { init -> expr(init, values, types) }
		val hir = Stmt.Let(pattern, type, initializer, stmt.ast)

		return Pair(hir, newValues)
	}

	/**
	 * @param stmt The DSR module converted into HIR.
	 * @return     The converted DSR module as a HIR module containing resolved names.
	 */
	private fun module(stmt: DSRStmt.Module): Stmt.Module {
		val values = Values(stmt.values, 0, null)
		val types = Types(stmt.types, 0, null)
		return Stmt.Module(stmt.statements.map { stmt -> stmt(stmt, values, types).first })
	}

	/**
	 * @param stmt  The DSR struct converted into HIR.
	 * @param types The type scope being used to resolve the statement.
	 * @return      The converted DSR struct as a HIR struct containing resolved names.
	 */
	private fun struct(stmt: DSRStmt.Struct, types: Types): Stmt.Struct {
		val types = Types(stmt.genericParams?.branch ?: Branch(), 0, types)
		val whereClause = whereClause(stmt.genericParams, stmt.ast.whereClause, types)
		val fields = stmt.ast.fields.map { (token, type) -> Pair(token, type(type, types)) }
		return Stmt.Struct(stmt.genericParams, whereClause, fields, stmt.ast)
	}

	/**
	 * @param stmt  The DSR tuple struct converted into HIR.
	 * @param types The type scope being used to resolve the statement.
	 * @return      The converted DSR tuple struct as a HIR tuple struct containing resolved names.
	 */
	private fun tupleStruct(stmt: DSRStmt.TupleStruct, types: Types): Stmt.TupleStruct {
		val types = Types(stmt.genericParams?.branch ?: Branch(), 0, types)
		val whereClause = whereClause(stmt.genericParams, stmt.ast.whereClause, types)
		val fields = stmt.ast.fields.map { type -> type(type, types) }
		return Stmt.TupleStruct(stmt.genericParams, whereClause, fields, stmt.ast)
	}

	/**
	 * @param stmt  The DSR unit struct converted into HIR.
	 * @param types The type scope being used to resolve the statement.
	 * @return      The converted DSR unit struct as a HIR unit struct containing resolved names.
	 */
	private fun unitStruct(stmt: DSRStmt.UnitStruct, types: Types): Stmt.UnitStruct {
		val types = Types(stmt.genericParams?.branch ?: Branch(), 0, types)
		val whereClause = whereClause(stmt.genericParams, stmt.ast.whereClause, types)
		return Stmt.UnitStruct(stmt.genericParams, whereClause, stmt.ast)
	}

	/**
	 * @param genericParams The generic parameters that may contain type bounds included in the returned where clause.
	 * @param whereClause   The DSR where clause being converted into HIR.
	 * @param types         The type scope being used to resolve the statement.
	 * @return              The HIR where clause combining predicates in the generic parameters and the where clause.
	 */
	private fun whereClause(genericParams: DSRGenericParams?, whereClause: ASTWhereClause?, types: Types): WhereClause {
		val predicates = ArrayList<Pair<Type, TypeParamBounds>>()
		genericParams?.let { generics ->
			val predicatesToAdd = generics.params
				.zip(generics.ast.params.map { param -> param.bounds })
				.mapNotNull { (param, bounds) -> bounds?.let { Pair(Type.ADT(TypeItemRef.Normal(param, null)), typeParamBounds(bounds, types)) } }
			predicates.addAll(predicatesToAdd)
		}

		whereClause?.let { where ->
			val predicatesToAdd = where.clauseItems
				.map { clauseItem -> Pair(type(clauseItem.type, types), typeParamBounds(clauseItem.bounds, types)) }
			predicates.addAll(predicatesToAdd)
		}

		return WhereClause(predicates)
	}

	/**
	 * @param type  The AST type being converted into an HIR type.
	 * @param types The type scope used to resolve the AST type.
	 * @return      The converted AST type as a HIR type.
	 */
	private fun type(type: ASTType, types: Types): Type {
		return when (type) {
			is ASTType.Error -> Type.Error
			is ASTType.Path  -> typePath(type.path, types)?.let { item -> Type.ADT(item) } ?: Type.Error
			is ASTType.Tuple -> Type.Tuple(type.fields.map { field -> type(field, types) })
		}
	}

	/**
	 * @param bounds The AST type parameter bounds being converted into HIR.
	 * @param types  The type scope used to resolve the AST param bounds.
	 * @return       The converted AST type param bounds as HIR type param bounds.
	 */
	private fun typeParamBounds(bounds: ASTTypeParamBounds, types: Types): TypeParamBounds {
		val traits = bounds.bounds.map { path -> traitItem(path, types) }.filterNotNull()
		return TypeParamBounds(traits)
	}


	/**
	 * @param path  The normal complex path being resolved.
	 * @param types The type scope used to resolve the path.
	 * @param index The index in the path's body being converted into a TypeItemRef.
	 * @return      The converted type item reference or null if an error occurred
	 */
	private fun pathBody(body: List<ComplexSegment>, types: Types, index: Int = body.size - 1): TypeItemRef? {
		val segment = body[index]
		val generics = segment.generics?.let { genericArgs(it, types) }

		if (index == 0)
			return getItem(segment.id, types, "scope")?.let { item -> TypeItemRef.Normal(item, generics) }

		val previous = pathBody(body, types, index - 1) ?: return null
		return associatedTypeIn(previous, segment.id, generics)
	}


	/**
	 * @param path  The complex path being interpreted as a type path. Can be normal or qualified
	 * @param types The type scope used to resolve the path.
	 * @return      The type item reference if the item was found.
	 */
	private fun typePath(path: ComplexPath, types: Types): TypeItemRef? {
		return when (path) {
			is ComplexPath.Error     -> null
			is ComplexPath.Normal    -> typePath(path, types)
			is ComplexPath.Qualified -> qualifiedTypePath(path, types)
		}
	}

	/**
	 * @param path  The normal complex path being resolved.
	 * @param types The type scope used to resolve the path.
	 * @return      The type item reference of the item if the item was found.
	 */
	private fun typePath(path: ComplexPath.Normal, types: Types) = pathBody(path.body, types)
	private fun qualifiedTypePath(path: ComplexPath.Qualified, types: Types): TypeItemRef.Qualified? {
		val type = type(path.type, types)
		val first = path.body.first()

		if (path.trait == null) {
			zinc.reportCompileError(CompilerError.useQualifiedPath(first.id, path.range()))
			return null
		}

		val trait = traitItem(path.trait, types) ?: return null

		val generics = first.generics?.let { generics -> genericArgs(generics, types) }

		if (path.body.size > 1) {
			zinc.reportCompileError(CompilerError.useQualifiedPath(path.body[1].id, path.range()))
			return null
		}

		return TypeItemRef.Qualified(type, trait, first.id, generics)
	}

	/**
	 * @param path   The complex path being interpreted as a value path.
	 * @param values The value scope used to resolve the path.
	 * @param types  The type scope used to resolve the path.
	 * @return       The value item reference if the item was found.
	 */
	private fun valuePath(path: ComplexPath, values: Values, types: Types): ValueItemRef? {
		return when (path) {
			is ComplexPath.Error     -> null
			is ComplexPath.Normal    -> unqualifiedValuePath(path, values, types)
			is ComplexPath.Qualified -> qualifiedValuePath(path, values, types)
		}
	}

	private fun unqualifiedValuePath(path: ComplexPath.Normal, values: Values, types: Types): ValueItemRef? {
		val last = path.body.last()
		val generics = last.generics?.let { generics -> genericArgs(generics, types) }
		if (path.body.size == 1) {
			val item = getItem(last.id, values, "scope") ?: return null
			return ValueItemRef.Normal(item, generics)
		}

		val typeItem = pathBody(path.body, types, path.body.size - 2) ?: return null
		return associatedValueIn(typeItem, last.id, generics)
	}

	private fun qualifiedValuePath(path: ComplexPath.Qualified, values: Values, types: Types): ValueItemRef? {
		val type = type(path.type, types)
		val first = path.body.first()
		val firstGenerics = first.generics?.let { generics -> genericArgs(generics, types) }

		return when (path.body.size) {
			1    -> if (path.trait == null) {
				ValueItemRef.TypeAccess(type, first.id, firstGenerics)
			} else {
				ValueItemRef.Qualified(type, traitItem(path.trait, types) ?: return null, first.id, firstGenerics)
			}

			2    -> {
				val second = path.body[1]
				val secondGenerics = second.generics?.let { generics -> genericArgs(generics, types) }
				val trait = if (path.trait == null) {
					zinc.reportCompileError(CompilerError.useQualifiedPath(second.id, path.range()))
					return null
				} else traitItem(path.trait, types) ?: return null
				ValueItemRef.TypeAccess(
					Type.ADT(TypeItemRef.Qualified(type, trait, first.id, firstGenerics)),
					second.id,
					secondGenerics
				)
			}

			else -> {
				val second = path.body[1]
				zinc.reportCompileError(
					CompilerError.useQualifiedPath(
						if (path.trait == null) first.id else second.id,
						path.range()
					)
				)
				null
			}
		}
	}

	/**
	 * @param item        The type item reference which [name] is being searched for in.
	 * @param name        The name of the associated type being searched for.
	 * @param genericArgs The HIR generic args provided with the name.
	 */
	private fun associatedTypeIn(item: TypeItemRef, name: Token, genericArgs: GenericArgs?): TypeItemRef? {
		return when (item) {
			is TypeItemRef.Normal    -> when (item.type) {
				is Generic,
				is Struct,
				is TupleStruct,
				is UnitStruct -> {
					zinc.reportCompileError(
						CompilerError.itemDoesNotHaveAssociatedItems(
							name,
							item.type.name()
						)
					) // TODO: possibly swap this for a qualified error
					null
				}

				is Module     -> TypeItemRef.Normal(getItem(name, Types(item.type.types, 0, null), "module") ?: return null, genericArgs)
				is Trait      -> TODO("add associated item look up to traits in the DSR")
			}

			is TypeItemRef.Qualified -> { // pretty sure its impossible but just in case
				zinc.reportCompileError(
					CompilerError.useQualifiedPath(
						name,
						name.asRange()
					)
				)
				null
			}
		}
	}

	/**
	 * @param item        The value item reference which [name] is being searched for in.
	 * @param name        The name of the associated value being searched for.
	 * @param genericArgs The HIR generic args provided with the name.
	 */
	private fun associatedValueIn(item: TypeItemRef, name: Token, genericArgs: GenericArgs?): ValueItemRef? {
		return when (item) {
			is TypeItemRef.Normal    -> when (item.type) {
				is Generic,
				is Struct,
				is TupleStruct,
				is UnitStruct -> ValueItemRef.TypeAccess(Type.ADT(item), name, genericArgs)

				is Module     -> ValueItemRef.Normal(getItem(name, Values(item.type.values, 0, null), "module") ?: return null, genericArgs)
				is Trait      -> TODO("add associated item look up to traits in the DSR")
			}

			is TypeItemRef.Qualified -> { // pretty sure its impossible but just in case
				zinc.reportCompileError(
					CompilerError.useQualifiedPath(
						name,
						name.asRange()
					)
				)
				null
			}
		}
	}

	/**
	 * @param generics The AST generic arguments being converted into HIR.
	 * @param types    The type scope used to resolve the path.
	 * @return         The converted AST generic arguments as HIR generic arguments.
	 */
	private fun genericArgs(generics: ASTGenericArgs, types: Types) = GenericArgs(generics.types.map { type -> type(type, types) })

	/**
	 * @param path  The complex path being interpreted as a type path.
	 * @param types The type scope used to resolve the path.
	 * @return      A normal type item reference where the inner item must be a trait. TODO: enforce this with the type system
	 *
	 * A wrapper around [typePath] that makes sure the item referenced is a trait.
	 */
	private fun traitItem(path: ComplexPath, types: Types): TypeItemRef.Normal? {
		val item = typePath(path, types) ?: return null
		return when (item) {
			is TypeItemRef.Normal -> when (item.type) {
				is Trait -> item
				else     -> {
					zinc.reportCompileError(CompilerError.expectedTrait(path.range(), item.type.name()))
					null
				}
			}

			else                  -> {
				zinc.reportCompileError(CompilerError.expectedTrait(path.range(), "qualified item"))
				null
			}
		}
	}

	/**
	 * @param expr   The DSR expression being converted into HIR.
	 * @param values The value path used to resolve the expression.
	 * @param types  The type path used to resolve the expression.
	 * @return       The converted DSR expression as a HIR expression.
	 */
	private fun expr(expr: DSRExpr, values: Values, types: Types): Expr {
		return when (expr) {
			is DSRExpr.Binary   -> Expr.Binary(expr(expr.left, values, types), expr(expr.right, values, types), expr.ast)
			is DSRExpr.Break    -> breakExpr(expr, values, types)
			is DSRExpr.Call     -> call(expr, values, types)
			is DSRExpr.Closure  -> closure(expr, values, types)
			is DSRExpr.Continue -> continueExpr(expr)
			is DSRExpr.FieldGet -> Expr.FieldGet(
				expr(expr.callee, values, types),
				expr.ast.segment.id,
				expr.ast.segment.generics?.let { generics -> genericArgs(generics, types) },
				expr.ast
			)

			is DSRExpr.Group    -> group(expr, values, types)
			is DSRExpr.Index    -> Expr.Index(expr(expr.callee, values, types), expr.args.map { arg -> expr(arg, values, types) }, expr.ast)
			is DSRExpr.Block    -> block(expr, values, types)
			is DSRExpr.For      -> forExpr(expr, values, types)
			is DSRExpr.If       -> ifExpr(expr, values, types)
			is DSRExpr.Loop     -> loop(expr, values, types)
			is DSRExpr.While    -> whileExpr(expr, values, types)
			is DSRExpr.Literal  -> Expr.Literal(expr.ast)
			is DSRExpr.Match    -> match(expr, values, types)
			is DSRExpr.Path     -> Expr.Item(valuePath(expr.ast.path, values, types), expr.ast)
			is DSRExpr.Range    -> Expr.Range(
				expr.left?.let { left -> expr(left, values, types) },
				expr.right?.let { right -> expr(right, values, types) },
				expr.ast
			)

			is DSRExpr.Return   -> returnExpr(expr, values, types)
			is DSRExpr.Unary    -> Expr.Unary(expr(expr.right, values, types), expr.ast)
		}
	}

	/**
	 * @param call The DSR call expression being converted into HIR.
	 * @param values The value path used to resolve the expression.
	 * @param types  The type path used to resolve the expression.
	 * @return       A HIR call expression or a HIR method call expression if the callee of the expression was a field get expr.
	 */
	private fun call(call: DSRExpr.Call, values: Values, types: Types): Expr {
		return if (call.callee !is DSRExpr.FieldGet)
			Expr.Call(expr(call.callee, values, types), call.args.map { arg -> expr(arg, values, types) }, call.ast)
		else
			Expr.MethodCall(
				expr(call.callee.callee, values, types),
				call.callee.ast.segment.id,
				call.callee.ast.segment.generics?.let { generics -> genericArgs(generics, types) },
				call.args.map { arg -> expr(arg, values, types) },
				call.ast
			)
	}

	private fun closure(closure: DSRExpr.Closure, values: Values, types: Types): Expr.Closure {
		// TODO: do not allow return statements inside of closures
		val patterns = closure.patterns.map { pattern -> pattern(pattern, values, types) }
		val expr = expr(closure.expr, Values(closure.paramScope, values.indexSinceDecl + 1, values), types)
		return Expr.Closure(patterns, expr, closure.ast)
	}

	private fun breakExpr(breakExpr: DSRExpr.Break, values: Values, types: Types): Expr.Break {
		val value = breakExpr.value?.let { value -> expr(value, values, types) }
		return Expr.Break(value, breakExpr.ast)
	}

	private fun continueExpr(continueExpr: DSRExpr.Continue): Expr.Continue {
		return Expr.Continue(continueExpr.ast)
	}

	private fun group(expr: DSRExpr.Group, values: Values, types: Types): Expr {
		return if (expr.exprs.size == 1) expr(expr.exprs.first(), values, types)
		else Expr.Tuple(expr.exprs.map { field -> expr(field, values, types) }, expr.ast)
	}

	private fun block(expr: DSRExpr.Block, values: Values, types: Types): Expr.Block {
		var values = Values(expr.values, values.indexSinceDecl + 1, values)
		val types = Types(expr.types, types.indexSinceDecl + 1, types)
		return Expr.Block(expr.stmts.map { stmt ->
			val (stmt, newValues) = stmt(stmt, values, types)
			values = newValues
			stmt
		}, expr.ast)
	}

	private fun forExpr(expr: DSRExpr.For, values: Values, types: Types): Expr.For {
		val pattern = pattern(expr.pattern, values, types)
		val iterator = expr(expr.iterator, values, types)
		val block = block(expr.block, values, types)
		return Expr.For(pattern, iterator, block, expr.ast)
	}

	private fun ifExpr(expr: DSRExpr.If, values: Values, types: Types): Expr.If {
		val condition = expr(expr.condition, values, types)
		val then = block(expr.then, values, types)
		val elseBranch = expr.orElse?.let { orElse -> expr(orElse, values, types) }
		return Expr.If(condition, then, elseBranch, expr.ast)
	}

	private fun loop(expr: DSRExpr.Loop, values: Values, types: Types): Expr.Loop {
		val block = block(expr.block, values, types)
		return Expr.Loop(block, expr.ast)
	}

	private fun whileExpr(expr: DSRExpr.While, values: Values, types: Types): Expr.While {
		val condition = expr(expr.condition, values, types)
		val block = block(expr.then, values, types)
		return Expr.While(condition, block, expr.ast)
	}

	private fun match(expr: DSRExpr.Match, values: Values, types: Types): Expr.Match {
		val exprToMatch = expr(expr.expr, values, types)
		val branches = expr.branches.map { (patternValuesBranch, pattern, expr) ->
			val branchValues = Values(patternValuesBranch, values.indexSinceDecl + 1, values)
			val pattern = pattern(pattern, values, types)
			val expr = expr(expr, branchValues, types)
			Pair(pattern, expr)
		}
		return Expr.Match(exprToMatch, branches, expr.ast)
	}

	private fun returnExpr(expr: DSRExpr.Return, values: Values, types: Types): Expr.Return {
		val returnValue = expr.value?.let { expr -> expr(expr, values, types) }
		return Expr.Return(returnValue, expr.ast)
	}

	/**
	 * @param pattern The DSR pattern being converted into HIR.
	 * @param values  The value path used to resolve the expression.
	 * @param types   The type path used to resolve the expression.
	 * @return        The converted DSR pattern as a HIR pattern.
	 */
	private fun pattern(pattern: DSRPattern, values: Values, types: Types): Pattern {
		return when (pattern) {
			is DSRPattern.Literal  -> Pattern.Literal(pattern.ast.token)
			is DSRPattern.Tuple    -> Pattern.Tuple(pattern.fields.map { field -> pattern(field, values, types) }, pattern.ast)
			is DSRPattern.Variable -> Pattern.Variable(pattern.variable, pattern.ast)
			is DSRPattern.Wildcard -> Pattern.Wildcard(pattern.ast.token)
		}
	}

	/**
	 * @param identifier The token being used to get an item from the scope.
	 * @param branches   The scope being used.
	 * @param env        The name of the environment the item is being grabbed from. For example, "scope", or "module"
	 * @return           The item found in the scope or null if the item is not found.
	 *
	 * Reports a name not found error if a name is not found.
	 */
	private fun <T> getItem(identifier: Token, branches: BranchNode<T>, env: String): T? {
		if (branches.branch.data.containsKey(identifier.lexeme!!)) {
			val item = branches.branch.data[identifier.lexeme]
			return item
		}
		zinc.reportCompileError(CompilerError.nameNotFound(identifier.lexeme, identifier.asRange(), env))
		return null
	}
}