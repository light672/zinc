package com.light672.zinc.lang.compiler.core

import com.light672.zinc.lang.compiler.Scope
import com.light672.zinc.lang.compiler.Type
import com.light672.zinc.lang.compiler.item.names.Struct

internal object Core {
	private val numStruct = Struct("num", HashMap(), ArrayList(), 0)
	val numType = Type.Primitive(numStruct)
	private val strStruct = Struct("str", HashMap(), ArrayList(), 0)
	val strType = Type.Primitive(strStruct)
	private val boolStruct = Struct("bool", HashMap(), ArrayList(), 0)
	val boolType = Type.Primitive(boolStruct)
	private val charStruct = Struct("char", HashMap(), ArrayList(), 0)
	val charType = Type.Primitive(charStruct)

	val namespace = Scope(HashMap(), HashMap(), HashMap(), 0, null).also {
		/* it.structs["num"] = Struct(true)
		it.structs["str"] = Struct(true)
		it.structs["bool"] = Struct(true)
		it.structs["char"] = Struct(true)

		it.implements[numType] = Implement(numType).also {
			it.traits[Ops.addTrait] = Implement.TraitImplement().also {
				it.methods["add"] =
			}
		}*/
	}
}