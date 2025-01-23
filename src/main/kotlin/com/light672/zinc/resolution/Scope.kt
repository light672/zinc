package com.light672.zinc.resolution

import com.light672.zinc.ast.Token

internal class Scope private constructor(val types: Branch<TypeItem>, val values: Branch<ValueItem>, val label: Label) {
	constructor() : this(Branch(), Branch(), Label(null, null, null, 0))

	data class Branch<T>(val data: HashMap<CharSequence, T>, var parent: Branch<T>?, val depthSinceItem: Int) {
		constructor() : this(HashMap(), null, 0)
		constructor(parent: Branch<T>, depthSinceItem: Int) : this(HashMap(), parent, depthSinceItem)
	}


	fun newValues() = Scope(types, Branch(values, values.depthSinceItem + 1), label)
	fun newScope(label: Pair<Token?, Expr?> = Pair(null, null)) =
		Scope(
			Branch(types, types.depthSinceItem + 1),
			Branch(values, values.depthSinceItem + 1),
			Label(label.first, label.second, this.label, this.label.depthSinceItem + 1)
		)

	fun newItem() = Scope(Branch(types, 0), Branch(values, 0), Label(null, null, label, 0))
}