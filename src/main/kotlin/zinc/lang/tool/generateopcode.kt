package zinc.lang.tool

import java.io.PrintWriter

fun main() {
	val opcodes = """
		OP_CONST
		OP_TRUE
		OP_FALSE
		OP_NULL
		OP_POP
		
		OP_ADD
		OP_SUB
		OP_MUL
		OP_DIV
		OP_MOD
		OP_POW
		
		OP_ADD_ASSIGN
		OP_SUB_ASSIGN
		OP_MUL_ASSIGN
		OP_DIV_ASSIGN
		OP_MOD_ASSIGN
		OP_POW_ASSIGN
		
		
		OP_CALL
		OP_RETURN
		
	""".trimIndent()
	val writer = PrintWriter("src/main/kotlin/zinc/lang/opcode.kt", "UTF-8")
	writer.println("package zinc.lang\n")
	var continueAmount = 0
	for ((i, s) in opcodes.split("\n").withIndex()) {
		if (s.isBlank()) {
			continueAmount++
			writer.println()
			continue
		}
		writer.println("val $s: Byte = ${i - continueAmount}")
	}
	writer.close()
}