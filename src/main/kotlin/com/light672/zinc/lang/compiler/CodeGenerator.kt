package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.builtin.ZincNumber
import com.light672.zinc.builtin.ZincValue
import com.light672.zinc.lang.Chunk
import com.light672.zinc.lang.runtime.opcodes.*

internal class CodeGenerator(val runtime: Zinc.Runtime) {
	private val code = ArrayList<Byte>()
	private val constants = ArrayList<ZincValue>()
	private val ranges = ArrayList<IntRange>()

	fun number(value: ZincNumber, range: IntRange) {
		val number = value.value
		val int = number.toInt()
		if (int.toDouble() == number && int in 0..Short.MAX_VALUE) {
			val (a, b) = toBytes(int)
			addCode(range, OP_CREATE_NUM, a, b)
		} else {
			emitConstant(range, value)
		}
	}

	fun emit(value: ZincValue, range: IntRange) = emitConstant(range, value)
	fun pop(range: IntRange) = addCode(range, OP_POP)

	fun emitFalse(range: IntRange) = addCode(range, OP_FALSE)
	fun emitTrue(range: IntRange) = addCode(range, OP_TRUE)
	fun emitNone(range: IntRange) = addCode(range, OP_NONE)


	private fun emitConstant(range: IntRange, value: ZincValue) {
		constants.add(value)
		val (a, b) = toBytes(constants.size - 1)
		addCode(range, OP_CONST, a, b)
	}

	private fun emitJump(range: IntRange, instruction: Byte): Int {
		addCode(range, instruction, 0xFF.toByte(), 0xFF.toByte())
		return code.size - 2
	}

	private fun patchJump(offset: Int, range: IntRange): Unit? {
		val jump = code.size - offset - 2
		if (jump > Short.MAX_VALUE) {
			runtime.reportCompileError(CompilerError.OneRangeError(range, "Too much code to jump over."))
			return null
		}
		val (a, b) = toBytes(jump)
		code[offset] = a
		code[offset + 1] = b
		return Unit
	}


	private fun addCode(range: IntRange, vararg bytes: Byte) {
		for (b in bytes) {
			code.add(b)
			ranges.add(range)
		}
	}

	private fun addCode(range: IntRange, byte: Byte) {
		code.add(byte)
		ranges.add(range)
	}

	fun generateChunk(): Chunk {
		val byteArray = code.toByteArray()
		val constantArray = constants.toTypedArray()
		return Chunk(byteArray, constantArray, 0)
	}
}