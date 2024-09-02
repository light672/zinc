package com.light672.zinc.lang.compiler.ir.prelude

import com.light672.zinc.lang.compiler.ir.Item
import com.light672.zinc.lang.compiler.ir.Struct
import com.light672.zinc.lang.compiler.type_checking.Type

internal object Primitives {
	val INT = Struct("int", 0)
	val FLOAT = Struct("float", 0)
	val BOOL = Struct("bool", 0)
	val CHAR = Struct("char", 0)
	val STRING = Struct("string", 0)

	val INT_TYPE = Type.Normal(INT, emptyList())
	val FLOAT_TYPE = Type.Normal(FLOAT, emptyList())
	val BOOL_TYPE = Type.Normal(BOOL, emptyList())
	val CHAR_TYPE = Type.Normal(CHAR, emptyList())
	val STRING_TYPE = Type.Normal(STRING, emptyList())

	fun register(map: HashMap<String, Item>) {
		map["int"] = INT
		map["float"] = FLOAT
		map["bool"] = BOOL
		map["char"] = CHAR
		map["string"] = STRING
	}
}