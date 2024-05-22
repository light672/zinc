package com.light672.zinc.lang.compiler.core

import com.light672.zinc.lang.compiler.constructs.Scope
import com.light672.zinc.lang.compiler.constructs.Type

internal object Core {
	val numType = Type()
	val strType = Type()
	val boolType = Type()
	val charType = Type()

	val namespace = Scope().also {
		it.types["num"] = numType
		it.types["str"] = strType
		it.types["bool"] = boolType
		it.types["char"] = charType
	}
}