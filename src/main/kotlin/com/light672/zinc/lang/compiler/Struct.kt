package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Stmt

internal class Struct(val type: Type.Struct, val statement: Stmt.Struct) {
	override fun toString() = type.toString()
}