package com.light672.zinc.resolution

internal class Scope private constructor(val types: Branch<TypeItem>, val values: Branch<ValueItem>) {
	constructor() : this(Branch(), Branch())

	data class Branch<T>(val data: HashMap<CharSequence, T>, var parent: Branch<T>?, var depthSinceItem: Int) {
		constructor() : this(HashMap(), null, 0)
		constructor(parent: Branch<T>, depthSinceItem: Int) : this(HashMap(), parent, depthSinceItem)
	}

	fun newValues() = Scope(types, Branch(values, values.depthSinceItem + 1))
	fun newScope() = Scope(Branch(types, types.depthSinceItem + 1), Branch(values, values.depthSinceItem + 1))
	fun newItem() = Scope(Branch(types, 0), Branch(values, 0))
}