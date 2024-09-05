package com.light672.zinc.lang.compiler.ir

internal class Module(
	val name: String,
	val values: Namespace.Branch,
	val types: Namespace.Branch
) : Item() {
	val globalLets = ArrayList<IRStmt.LetBinding>()
}