package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.item.Implement
import com.light672.zinc.lang.compiler.item.names.Name
import com.light672.zinc.lang.compiler.item.names.Trait
import com.light672.zinc.lang.compiler.item.names.Variable

internal class Scope(
	private val names: HashMap<String, Name>,
	private val variables: HashMap<String, Variable>,
	private val implements: HashMap<Type, Implement>,
	val depth: Int,
	val parent: Scope?
) {
	fun addName(name: Name) {
		if (names.containsKey(name.name))
			throw Resolver.ResolverError("'${name.name}' already exists in current scope.")
		names[name.name] = name
	}

	fun addVariable(variable: Variable) {
		if (depth == 0 && variables.containsKey(variable.name))
			throw Resolver.ResolverError("Variable '${variable.name}' already exists in the current scope.")
		variables[variable.name] = variable
	}

	fun getVariable(name: String): Variable {
		var scope: Scope? = this
		while (scope != null) {
			val variable = variables[name]
			scope = scope.parent
			return variable ?: continue
		}
		throw Resolver.ResolverError("Variable '$name' not found in the current scope.")
	}

	fun getName(name: String): Name {
		var scope: Scope? = this
		while (scope != null) {
			val name = names[name]
			scope = scope.parent
			return name ?: continue
		}
		throw Resolver.ResolverError("Variable '$name' not found in the current scope.")
	}

	fun impl(type: Type, trait: Trait, associatedFunctions: ArrayList<Pair<String, Type.Function>>) {
		when (type) {
			is Type.Tuple, is Type.Function, is Type.Trait ->
				throw Resolver.ResolverError("Can only implement on structs, tuple structs, enums, and primitives.")

			is Type.Primitive -> {
				if (trait !== Trait.DEFAULT_TRAIT && implements.containsKey(type))
					throw Resolver.ResolverError("Conflicting implementations of trait '$trait' for type '$type'.")
				val traitImpl = traitImplOnType(type, trait)

				for ((name, function) in associatedFunctions) {
					try {
						traitImpl.addMethod(name, function)
					} catch (error: Resolver.ResolverError) {
						continue
					}
				}
			}

			is Type.InitializedStruct -> {
				val impl = implOnType(type)
				for (existingTrait in impl.traits.keys) {

				}
			}

			Type.Generic -> TODO()
		}
	}

	fun impl(type: Type, associatedType: String) {
		when (type) {
			is Type.Tuple, is Type.Function, is Type.Trait ->
				throw Resolver.ResolverError("Can only implement on structs, tuple structs, enums, and primitives.")

			is Type.Primitive -> {

			}

			is Type.InitializedStruct -> {

			}

			Type.Generic -> TODO()
		}
	}

	private fun traitImplOnType(type: Type, trait: Trait): Implement.TraitImplement {
		val impl = implOnType(type)
		val traitImpl = impl.traits[trait] ?: run {
			val traitImpl = Implement.TraitImplement(type, trait)
			impl.traits[trait] = traitImpl
			traitImpl
		}
		return traitImpl
	}

	private fun implOnType(type: Type): Implement {
		return implements[type] ?: run {
			val impl = Implement(type, depth)
			implements[type] = impl
			impl
		}
	}

	fun clone() = Scope(
		names.clone() as HashMap<String, Name>,
		variables.clone() as HashMap<String, Variable>,
		implements.clone() as HashMap<Type, Implement>,
		0,
		null
	)
}