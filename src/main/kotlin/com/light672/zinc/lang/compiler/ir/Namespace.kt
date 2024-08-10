package com.light672.zinc.lang.compiler.ir

import com.light672.zinc.Zinc
import com.light672.zinc.lang.compiler.ast.syntax.Pattern
import com.light672.zinc.lang.compiler.ast.syntax.Stmt
import com.light672.zinc.lang.compiler.ast.syntax.Token
import com.light672.zinc.lang.tool.Either

internal class Namespace(private val zinc: Zinc.Runtime) {


	var types = Branch(this)
	var values = Branch(this)

	val rootModule = Module("root", types, values)

	private var typeDepth = 0
	private var valueDepth = 0

	private fun newValues(allowedLocals: Boolean, code: () -> Unit): Branch {
		val oldValues = values
		val newValues = Branch(oldValues, allowedLocals)
		values = newValues
		valueDepth++
		code()
		values = oldValues
		valueDepth--
		return newValues
	}

	private fun newTypes(allowedLocals: Boolean, code: () -> Unit): Branch {
		val oldTypes = types
		val newTypes = Branch(oldTypes, allowedLocals)
		types = newTypes
		typeDepth++
		code()
		types = oldTypes
		typeDepth--
		return newTypes
	}

	fun newValues(branch: Branch, code: () -> Unit) {
		val oldValues = values
		values = branch
		valueDepth++
		code()
		values = oldValues
		valueDepth--
	}

	fun newTypes(branch: Branch, code: () -> Unit) {
		val oldTypes = types
		types = branch
		typeDepth++
		code()
		types = oldTypes
		typeDepth--
	}

	fun addItem(declaration: Stmt) {
		when (declaration) {
			is Stmt.Function -> {
				lateinit var innerValues: Branch
				val innerTypes = newTypes(true) {
					innerValues = newValues(true) {
						when (declaration.scOrBlock) {
							is Either.Left -> {}
							is Either.Right -> for (statement in declaration.scOrBlock.value.stmts) addItem(statement)
						}
					}
				}

				val function = Function(declaration.name.lexeme, declaration.generics?.params?.size ?: 0, declaration, innerValues, innerTypes)
				values.put(declaration.name.lexeme, function, declaration.range())
			}

			is Stmt.Module -> {
				lateinit var innerValues: Branch
				val innerTypes = newTypes(false) {
					innerValues = newValues(false) {
						for (statement in declaration.stmts) {
							addItem(declaration)
						}
					}
				}
				val module = Module(declaration.name.lexeme, innerValues, innerTypes)
				types.put(declaration.name.lexeme, module, declaration.range())
			}

			is Stmt.TypeAlias -> {
				types.put(
					declaration.name.lexeme,
					TypeAlias(declaration.name.lexeme, declaration.genericParams?.params?.size ?: 0),
					declaration.range()
				)
			}

			is Stmt.Variable -> {
				if (valueDepth == 0) {
					val variables = declaration.pattern.toVariables()
					for (variable in variables) values.put(variable.name, variable, declaration.range())
				}
			}

			is Stmt.Expression -> {}
		}
	}

	fun addPatternLocals(pattern: Pattern, irPattern: IRPattern) {
		when (irPattern) {
			is IRPattern.Identifier -> values.put(irPattern.variable.name, irPattern.variable, pattern.range())
		}
	}


	class Branch : Iterable<Item> {
		constructor(parent: Branch, allowedLocals: Boolean) {
			this.parent = parent
			namespace = parent.namespace
			zinc = parent.zinc
		}

		constructor(namespace: Namespace) {
			parent = null
			this.namespace = namespace
			this.zinc = namespace.zinc
		}


		private val parent: Branch?
		private val namespace: Namespace
		private val zinc: Zinc.Runtime

		private val names = HashMap<String, Item>()

		fun put(name: String, item: Item, errorRange: Token.Range) {
			val previous = names[name]
			names[name] = item
			if (previous != null) {
				zinc.reportCompileError("'$name' already declared in the current scope.", errorRange)
			}
		}

		fun get(name: String): Item? {
			return names[name]
		}

		fun recursiveGet(name: String): Item? {
			return names[name] ?: parent?.recursiveGet(name)
		}

		override fun iterator(): Iterator<Item> {
			return names.values.iterator()
		}
	}
}