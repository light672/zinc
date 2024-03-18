package com.light672.zinc.lang.tool

import java.io.PrintWriter

fun main() {
	val opcodes = """
		OP_CONST /*[short]*/
		OP_CREATE_NUM /*[short]*/
		OP_ALLOC /*[byte]*/
		OP_NONE
		OP_TRUE
		OP_FALSE
		OP_POP
		
		OP_ADD
		OP_SUB
		OP_DIV
		OP_MUL
		OP_MOD
		OP_POW
		
		OP_NOT
		OP_NEG
		
		OP_JMP /*[short]*/
		OP_JIF /*[short]*/
		OP_JIT /*[short]*/
		
		OP_GET_STACK /*[byte]*/
		OP_SET_STACK /*[byte]*/
		OP_GET_IND
		OP_SET_IND
		
		OP_CREATE_FUNCTION /*[byte]*/
		
		OP_CALL
		OP_CALL_NATIVE
		OP_RETURN
		OP_RETURN_VALUE
		OP_END
	""".trimIndent()
	val writer = PrintWriter("src/main/kotlin/com/light672/zinc/lang/runtime/opcodes/opcode.kt", "UTF-8")
	writer.println("package com.light672.zinc.lang.runtime.opcodes\n")
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