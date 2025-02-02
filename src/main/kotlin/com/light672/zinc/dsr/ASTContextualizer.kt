package com.light672.zinc.dsr

import com.light672.zinc.CompilerError
import com.light672.zinc.Zinc
import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Expr as ASTExpr
import com.light672.zinc.ast.FunctionParam as ASTFunctionParam
import com.light672.zinc.ast.GenericParams as ASTGenericParams
import com.light672.zinc.ast.Pattern as ASTPattern
import com.light672.zinc.ast.Stmt as ASTStmt

internal class ASTContextualizer(val zinc: Zinc.Runtime) {

	/**
	 * @param stmt   The AST statement being declared in scope and converted into DSR.
	 * @param types  The type branch of the scope the statement is being declared in.
	 * @param values The value branch of the scope the statement is being declared in.
	 * @param env    The name of the environment the statement is being declared in. For example "scope" or "module". Used for error purposes.
	 * @return       The converted AST statement as a DSR statement containing scope and item information.
	 */
	private fun stmt(stmt: ASTStmt, types: Branch<TypeItem>, values: Branch<ValueItem>, env: String): Stmt {
		return when (stmt) {
			is ASTStmt.Module -> {
				val item = Module(stmt)
				addToBranch(stmt.name.lexeme!!, stmt.range(), item, types, env)
				module(stmt)
			}

			is ASTStmt.Trait -> {
				val item = Trait(stmt)
				addToBranch(stmt.name.lexeme!!, stmt.range(), item, types, env)
				trait(stmt)
			}

			is ASTStmt.Struct -> {
				val item = Struct(stmt)
				addToBranch(stmt.name.lexeme!!, stmt.range(), item, types, env)
				struct(stmt)
			}

			is ASTStmt.TupleStruct -> {
				val item = TupleStruct(stmt)
				addToBranch(stmt.name.lexeme!!, stmt.range(), item, types, env)
				tupleStruct(stmt)
			}

			is ASTStmt.UnitStruct -> {
				val item = UnitStruct(stmt)
				addToBranch(stmt.name.lexeme!!, stmt.range(), item, types, env)
				addToBranch(stmt.name.lexeme, stmt.range(), item, values, env)
				unitStruct(stmt)
			}

			is ASTStmt.Function -> {
				val item = Function(stmt)
				addToBranch(stmt.name.lexeme!!, stmt.range(), item, values, env)
				function(stmt)
			}

			is ASTStmt.Expression -> Stmt.Expression(expr(stmt.expr), stmt)


			is ASTStmt.Implementation -> implementation(stmt)
			is ASTStmt.Let -> let(stmt)


		}
	}


	/**
	 * @param module The AST module statement being converted into DSR.
	 * @return       The converted AST module as a DSR module containing scope information.
	 */
	private fun module(module: ASTStmt.Module): Stmt.Module {
		val types = Branch<TypeItem>()
		val values = Branch<ValueItem>()
		val stmts = module.statements.map { stmt -> stmt(stmt, types, values, "module") }
		return Stmt.Module(types, values, stmts, module)
	}

	/**
	 * @param trait The AST trait statement being converted into DSR.
	 * @return      The converted AST trait as a DSR trait containing scope information.
	 */
	private fun trait(trait: ASTStmt.Trait): Stmt.Trait {
		val genericParams = trait.genericParams?.let { params -> genericParams(params) }
		val stmts = trait.statements.map { stmt ->
			when (stmt) {
				is ASTStmt.Function -> function(stmt, genericParams?.branch)
			}
		}
		return Stmt.Trait(genericParams, stmts, trait)
	}

	/**
	 * @param struct The AST struct statement being converted into DSR.
	 * @return       The converted AST struct as a DSR struct containing scope information.
	 */
	private fun struct(struct: ASTStmt.Struct): Stmt.Struct {
		val genericParams = struct.genericParams?.let { params -> genericParams(params) }
		return Stmt.Struct(genericParams, struct)
	}

	/**
	 * @param struct The AST unit struct statement being converted into DSR.
	 * @return       The converted AST unit struct as a DSR unit struct containing scope information.
	 */
	private fun unitStruct(struct: ASTStmt.UnitStruct): Stmt.UnitStruct {
		val genericParams = struct.genericParams?.let { params -> genericParams(params) }
		return Stmt.UnitStruct(genericParams, struct)
	}

	/**
	 * @param struct The AST tuple struct statement being converted into DSR.
	 * @return       The converted AST tuple struct as a DSR tuple struct containing scope information.
	 */
	private fun tupleStruct(struct: ASTStmt.TupleStruct): Stmt.TupleStruct {
		val genericParams = struct.genericParams?.let { params -> genericParams(params) }
		return Stmt.TupleStruct(genericParams, struct)
	}

	/**
	 * @param function            The AST function statement being converted into DSR.
	 * @param parentGenericBranch The type branch containing the generic parameters defined by an implementation or trait. For functions declared outside impls or traits, its default value is null.
	 * @return                    The converted AST function as a DSR function containing scope information.
	 */
	private fun function(function: ASTStmt.Function, parentGenericBranch: Branch<TypeItem>? = null): Stmt.Function {
		val genericParams = function.genericParams?.let { params -> genericParams(params, parentGenericBranch) }
		val paramBranch = Branch<ValueItem>()
		val params = function.params.map { param ->
			when (param) {
				is ASTFunctionParam.Pattern -> FunctionParam.Pattern(pattern(param.pattern, paramBranch), param)
				is ASTFunctionParam.Self -> {
					val selfItem = Self(param)
					addToBranch("self", param.self.asRange(), selfItem, paramBranch, "function parameters")
					FunctionParam.Self(selfItem, param)
				}
			}
		}
		val block = function.block?.let { block -> block(block) }
		return Stmt.Function(genericParams, paramBranch, params, block, function)
	}

	/**
	 * @param impl The AST implementation statement being converted into DSR.
	 * @return     The converted AST implementation as a DSR implementation containing scope information.
	 */
	private fun implementation(impl: ASTStmt.Implementation): Stmt.Implementation {
		val genericParams = impl.genericParams?.let { params -> genericParams(params) }
		val stmts = impl.statements.map { stmt ->
			when (stmt) {
				is ASTStmt.Function -> function(stmt, genericParams?.branch)
			}
		}

		return Stmt.Implementation(genericParams, stmts, impl)
	}

	/**
	 * @param let The AST let statement being converted into DSR.
	 */
	private fun let(let: ASTStmt.Let): Stmt.Let {
		val branch = Branch<ValueItem>()
		val pattern = pattern(let.pattern, branch)
		val initializer = let.initializer?.let { expr -> expr(expr) }
		return Stmt.Let(branch, pattern, initializer, let)
	}


	/**
	 * @param genericParams       The AST generic parameters being converted into DSR.
	 * @param parentGenericBranch The type branch containing the generic parameters defined by an implementation or trait if inside one.
	 * @return                    The converted AST generic parameters as DSR generic parameters containing generic items.
	 */
	private fun genericParams(genericParams: ASTGenericParams, parentGenericBranch: Branch<TypeItem>? = null): GenericParams {
		val genericScope = Branch<TypeItem>()
		val generics = genericParams.params.withIndex().map { (index, param) ->
			val item = Generic(index, param.name)
			addGenericToBranch(param.name, item, genericScope, parentGenericBranch)
			item
		}

		return GenericParams(generics, genericScope, genericParams)
	}

	/**
	 * @param expr The AST expression being declared in scope and converted into DSR.
	 * @return     The converted AST expression as a DSR expression containing scope and item information.
	 */
	private fun expr(expr: ASTExpr): Expr {
		return when (expr) {
			is ASTExpr.Binary -> Expr.Binary(expr(expr.left), expr(expr.right), expr)
			is ASTExpr.Block -> block(expr)
			is ASTExpr.Break -> Expr.Break(expr.expr?.let { value -> expr(value) }, expr)
			is ASTExpr.Call -> Expr.Call(expr(expr.callee), expr.args.map { arg -> expr(arg) }, expr)
			is ASTExpr.Closure -> closure(expr)
			is ASTExpr.Continue -> Expr.Continue(expr)
			is ASTExpr.FieldGet -> Expr.FieldGet(expr(expr.callee), expr)
			is ASTExpr.For -> forExpr(expr)
			is ASTExpr.Group -> Expr.Group(expr.expressions.map { field -> expr(field) }, expr)
			is ASTExpr.If -> Expr.If(expr(expr.condition), block(expr.thenBlock), expr.elseExpr?.let { elseExpr -> expr(elseExpr) }, expr)
			is ASTExpr.Index -> Expr.Index(expr(expr.callee), expr.args.map { arg -> expr(arg) }, expr)
			is ASTExpr.Literal -> Expr.Literal(expr)
			is ASTExpr.Loop -> Expr.Loop(block(expr.block), expr)
			is ASTExpr.Match -> match(expr)
			is ASTExpr.Path -> Expr.Path(expr)
			is ASTExpr.Range -> Expr.Range(expr.left?.let { left -> expr(left) }, expr.right?.let { right -> expr(right) }, expr)
			is ASTExpr.Return -> Expr.Return(expr.expr?.let { value -> expr(value) }, expr)
			is ASTExpr.Unary -> Expr.Unary(expr(expr.right), expr)
			is ASTExpr.While -> Expr.While(expr(expr.condition), block(expr.block), expr)
		}
	}

	/**
	 * @param block The AST block expression being converted into DSR.
	 * @return      The converted AST block as a DSR block containing scope information.
	 */
	private fun block(block: ASTExpr.Block): Expr.Block {
		val types = Branch<TypeItem>()
		val values = Branch<ValueItem>()
		val stmts = block.stmts.map { stmt ->
			stmt(stmt, types, values, "scope")
		}
		return Expr.Block(types, values, stmts, block)
	}

	/**
	 * @param closure The AST closure expression being converted into DSR.
	 * @return        The converted AST closure as a DSR closure containing scope information.
	 */
	private fun closure(closure: ASTExpr.Closure): Expr.Closure {
		val paramBranch = Branch<ValueItem>()
		val patterns = closure.params.map { (pattern, type) -> pattern(pattern, paramBranch) }
		val expr = expr(closure.expr)
		return Expr.Closure(paramBranch, patterns, expr, closure)
	}

	/**
	 * @param forExpr The AST for expression being converted into DSR.
	 * @return        The converted AST for loop as a DSR for loop containing scope information.
	 */
	private fun forExpr(forExpr: ASTExpr.For): Expr.For {
		val patternBranch = Branch<ValueItem>()
		val pattern = pattern(forExpr.pattern, patternBranch)
		val iterator = expr(forExpr.iterator)
		val block = block(forExpr.block)
		return Expr.For(patternBranch, pattern, iterator, block, forExpr)
	}

	/**
	 * @param match The AST match expression being converted into DSR.
	 * @return      The converted AST match as a DSR match containing scope information.
	 */
	private fun match(match: ASTExpr.Match): Expr.Match {
		val expr = expr(match.expr)
		val branches = match.branches.map { (pattern, expr) ->
			val patternBranch = Branch<ValueItem>()
			val pattern = pattern(pattern, patternBranch)
			val expr = expr(expr)
			Triple(patternBranch, pattern, expr)
		}

		return Expr.Match(expr, branches, match)
	}


	/**
	 * @param pattern The pattern being converted into DSR.
	 * @param branch  The value branch where any variables declared by the pattern are added.
	 * @return        The converted AST pattern as a DSR pattern containing variable items.
	 */
	private fun pattern(pattern: ASTPattern, branch: Branch<ValueItem>): Pattern {
		return when (pattern) {
			is ASTPattern.Identifier -> {
				val variable = Variable(pattern)
				addToBranch(pattern.token.lexeme!!, pattern.token.asRange(), variable, branch, "scope")
				Pattern.Variable(variable, pattern)
			}

			is ASTPattern.Literal -> Pattern.Literal(pattern)
			is ASTPattern.Tuple -> Pattern.Tuple(pattern.fields.map { field -> pattern(field, branch) }, pattern)
			is ASTPattern.Wildcard -> Pattern.Wildcard(pattern)
		}
	}


	/**
	 * @param name      The name of the item being declared in [branch].
	 * @param declRange The token range of the declaration statement. Used for error purposes.
	 * @param item      The item being declared under [name] in [branch]. If you are adding a generic, use [addGenericToBranch].
	 * @param branch    The branch in which the item is declared.
	 * @param env       The name of the environment the item is being declared in. For example "scope" or "module". Used for error purposes.
	 *
	 * Reports an error and sets the item under the name as null if an item with the same name already exists in the branch.
	 */
	private fun <T> addToBranch(name: CharSequence, declRange: Token.Range, item: T, branch: Branch<T>, env: String) {
		val toAdd =
			if (branch.data.containsKey(name)) {
				zinc.reportCompileError(CompilerError.nameAlreadyExists(name, declRange, env))
				null
			} else item

		branch.data[name] = toAdd
	}

	/**
	 * @param name                The name of the generic declared in the branch
	 * @param item                The generic being declared under [name] in [branch]
	 * @param branch              The branch in which the generic is declared
	 * @param parentGenericBranch The type branch containing the generic parameters defined by an implementation or trait if inside one.
	 *
	 * Reports an error and sets the item under the name as null if an item with the same name already exists in the branch or in the parent generic branch.
	 */
	private fun addGenericToBranch(name: Token, item: Generic, branch: Branch<TypeItem>, parentGenericBranch: Branch<TypeItem>?) {
		val toAdd =
			if (branch.data.containsKey(name.lexeme!!)) {
				zinc.reportCompileError(CompilerError.nameAlreadyExists(name.lexeme, name.asRange(), "generic parameters"))
				null
			} else {
				parentGenericBranch?.let { parentBranch ->
					if (parentBranch.data.containsKey(name.lexeme))
						zinc.reportCompileError(CompilerError.usedGenericParamNameInItem(name))
				}
				item
			}

		branch.data[name.lexeme] = toAdd
	}
}