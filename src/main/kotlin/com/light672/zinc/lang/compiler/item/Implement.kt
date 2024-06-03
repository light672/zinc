package com.light672.zinc.lang.compiler.item

import com.light672.zinc.lang.compiler.Resolver
import com.light672.zinc.lang.compiler.Type
import com.light672.zinc.lang.compiler.item.names.Trait

internal class Implement(val type: Type, depth: Int) : Item(depth) {
	val traits = HashMap<Trait, TraitImplement>()

	class TraitImplement(val type: Type, val trait: Trait) {
		private val methods = HashMap<String, Type.Function>()
		private val typeVariables = HashMap<String, Type>()
		private val generics = ArrayList<Type>()

		fun addMethod(name: String, function: Type.Function) {
			val method = methods[name]
			val associatedFunction = trait.associatedFunctions[name]

			if (trait === Trait.DEFAULT_TRAIT) {
				if (method != null)
					throw Resolver.ResolverError("Associated function '$name' already implemented on type '$type'.")
			} else {
				if (associatedFunction == null)
					throw Resolver.ResolverError("Associated function '$name' is not a member of trait '$trait'.")
				if (method != null)
					throw Resolver.ResolverError("Associated function '$name' already implemented on type '$type' for trait '$trait'.")
			}
			methods[name] = function
		}

		fun getMethods() = methods as Map<String, Type.Function>
	}

	fun findMethod(name: String): Type.Function {
		val def = traits[Trait.DEFAULT_TRAIT]
		if (def != null)
			if (def.getMethods().containsKey(name)) return def.getMethods()[name]!!

		var instances = 0
		var finalInstance: Type.Function? = null
		for (entry in traits) {
			if (entry.key === Trait.DEFAULT_TRAIT) continue
			if (entry.value.getMethods().containsKey(name)) {
				instances++
				finalInstance = entry.value.getMethods()[name]!!
			}
		}
		if (instances > 1)
			throw Resolver.ResolverError("Several instances of method '$name' have been implemented on type '$type'.")
		if (finalInstance == null)
			throw Resolver.ResolverError("No method '$name' implemented on type '$type'.")

		return finalInstance
	}
}