package com.light672.zinc.lang.tool

import java.io.PrintWriter

fun main() {
	val s = StringBuilder()
	val opcodes = """
		OP_CONST
		OP_TRUE
		OP_FALSE
		OP_POP
		
		OP_ADD
		OP_SUB
		OP_DIV
		OP_MUL
		OP_MOD
		OP_POW
		
		OP_CALL
		OP_CALL_NATIVE
		OP_RETURN
		OP_END
	""".trimIndent()
	val writer = PrintWriter("src/main/kotlin/zinc/lang/runtime/opcode.kt", "UTF-8")
	writer.println("package zinc.lang.runtime\n")
	var continueAmount = 0
	for ((i, s) in opcodes.split("\n").withIndex()) {
		if (s.isBlank()) {
			continueAmount++
			writer.println()
			continue
		}
		writer.println("internal const val $s: Byte = ${i - continueAmount}")
	}
	writer.close()
}