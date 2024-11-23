package com.light672.zinc

import com.light672.zinc.ast.Token
import com.light672.zinc.ir.IRType
import com.light672.zinc.item.Implementation
import com.light672.zinc.item.TypeItem
import com.light672.zinc.item.ValueItem

internal data class ScopeInfo(
	val types: Branch<TypeItem>,
	val values: Branch<ValueItem>,
	val impls: ImplBranch,
	val interfaceImpls: InterfaceImplBranch,
	val implementationItems: ArrayList<Implementation>
) {

	constructor(zinc: Zinc.Runtime) : this(Branch(zinc), Branch(zinc), ImplBranch(zinc), InterfaceImplBranch(zinc), ArrayList())

	class Branch<T>(private val zinc: Zinc.Runtime) : Iterable<Map.Entry<CharSequence, T>> {
		private var parent: Branch<T>? = null
		private val names = HashMap<CharSequence, T>()

		fun add(name: Token, item: T, override: Boolean = true) {
			if (names.containsKey(name.lexeme)) {
				zinc.reportCompileError(CompilerError.nameAlreadyExists(name))
			}
			if (override)
				names[name.lexeme] = item
		}

		fun get(name: Token, reportError: Boolean = true): T? {
			val item = names[name.lexeme] ?: parent?.get(name, reportError)
			if (item == null && parent == null && reportError)
				zinc.reportCompileError(CompilerError.nameDoesNotExist(name))
			return item
		}

		fun bindParent(parent: Branch<T>) {
			this.parent = parent
		}


		override fun iterator() = names.iterator()
	}

	class ImplBranch(private val zinc: Zinc.Runtime) {
		private var parent: ImplBranch? = null
		private val implementations = HashMap<IRType, Branch<ValueItem>>()

		fun getImplementation(type: IRType): Branch<ValueItem>? {
			val current = implementations[type]
			current?.let { return current }
			return parent?.let { it.implementations[type] ?: it.getImplementation(type) }
		}

		fun get(type: IRType, name: Token, reportError: Boolean = true): ValueItem? {
			val branch = getImplementation(type) ?: return null
			val item = branch.get(name, reportError) ?: parent?.get(type, name, reportError)
			if (item == null && parent == null && reportError)
				zinc.reportCompileError(CompilerError.nameIsNotMemberOf(name, type)) // change error later
			return item
		}

		fun add(type: IRType, name: Token, value: ValueItem) {
			val branch = implementations[type] ?: Branch<ValueItem>(zinc).also { branch ->
				getImplementation(type)
					?.let { parentBranch -> branch.bindParent(parentBranch) }
			}
			implementations[type] = branch
			branch.add(name, value)
		}

		fun bindParent(parent: ImplBranch) {
			this.parent = parent
		}
	}


	class InterfaceImplBranch(private val zinc: Zinc.Runtime) {
		private var parent: InterfaceImplBranch? = null
		private val implementations = HashMap<Pair<IRType, IRType>, Branch<ValueItem>>()


		fun getImplementation(type: IRType, inheritedType: IRType): Branch<ValueItem>? {
			val pair = Pair(type, inheritedType)
			val current = implementations[pair]
			current?.let { return current }
			return parent?.let { it.implementations[pair] ?: it.getImplementation(type, inheritedType) }
		}

		fun get(type: IRType, inheritedType: IRType, name: Token, reportError: Boolean = true): ValueItem? {
			val branch = getImplementation(type, inheritedType) ?: return null
			val item = branch.get(name, reportError) ?: parent?.get(type, inheritedType, name, reportError)
			if (item == null && parent == null && reportError)
				zinc.reportCompileError(CompilerError.nameIsNotMemberOf(name, inheritedType))
			return item
		}

		fun add(type: IRType, inheritedType: IRType, name: Token, value: ValueItem) {
			val branch = implementations[Pair(type, inheritedType)] ?: Branch<ValueItem>(zinc).also { branch ->
				getImplementation(type, inheritedType)
					?.let { parentBranch -> branch.bindParent(parentBranch) }
			}
			implementations[Pair(type, inheritedType)] = branch
			branch.add(name, value)
		}

		fun bindParent(parent: InterfaceImplBranch) {
			this.parent = parent
		}
	}
}