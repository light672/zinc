package com.light672.zinc.lang.compiler.ir.prelude

import com.light672.zinc.lang.compiler.ir.Item
import com.light672.zinc.lang.compiler.ir.Struct

internal object Primitives {
	val INT = Struct("int", 0)
	val FLOAT = Struct("float", 0)
	val BOOL = Struct("bool", 0)
	val CHAR = Struct("char", 0)
	val STRING = Struct("string", 0)

	fun register(map: HashMap<String, Item>) {
		map["int"] = INT
		map["float"] = FLOAT
		map["bool"] = BOOL
		map["char"] = CHAR
		map["string"] = STRING
	}
}