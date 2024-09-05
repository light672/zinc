package com.light672.zinc.lang.compiler.type_checking

import com.light672.zinc.Zinc
import com.light672.zinc.lang.compiler.ir.*
import com.light672.zinc.lang.compiler.ir.Function
import com.light672.zinc.lang.compiler.ir.prelude.Primitives
import com.light672.zinc.lang.tool.Either

internal class TypeChecker(val namespace: Namespace, private val zinc: Zinc.Runtime) {
	private var currentModule: Module = namespace.rootModule

	private fun check() {
		for (type in namespace.types) check(type)
		for (value in namespace.values) check(value)
	}

	private fun check(item: Item) {
		when (item) {
			is Function -> checkFunction(item)
			is Module -> checkModule(item)
			is Struct -> checkStruct(item)
			is TypeAlias -> TODO()
			is Static -> checkStatic(item)
			is Variable -> {}
		}
	}

	private fun checkFunction(function: Function) {
		when (function.block) {
			is Either.Left -> {}
			is Either.Right -> {
				val block = (function.block as Either.Right<Unit, IRExpr.Block>).value
				namespace.withTypes(block.ast.types) {
					namespace.withValues(block.ast.values) {
						check()
						for (statement in block.stmts) {
							check(statement)
						}
					}
				}
			}
		}
	}

	private fun checkModule(module: Module) {
		namespace.withTypes(module.types) {
			namespace.withValues(module.values) {
				check()
			}
		}
	}

	private fun checkStruct(struct: Struct) {
		TODO()
	}

	private fun checkStatic(static: Static) {

	}

	private fun check(statement: IRStmt) {

	}

	private fun check(expression: IRExpr): Type {
		return when (expression) {
			is IRExpr.Binary -> {
				val aType = check(expression.left)
				val bType = check(expression.right)
				if (aType == bType && (aType == Primitives.INT_TYPE) || (aType == Primitives.FLOAT_TYPE)) {
					aType
				} else {
					return Type.ERROR
				}
			}

			is IRExpr.Block -> TODO()
			is IRExpr.Function -> TODO()
			is IRExpr.Literal -> TODO()
			is IRExpr.Unary -> TODO()
			is IRExpr.Variable -> TODO()
		}
	}


	private fun checkPattern(type: Type, pattern: IRPattern) {
		when (pattern) {
			is IRPattern.Identifier -> {
				pattern.variable.type = type
			}
		}
	}
}