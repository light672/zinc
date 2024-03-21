package com.light672.zinc.lang.runtime

import com.light672.zinc.builtin.*
import com.light672.zinc.lang.Chunk
import com.light672.zinc.lang.compiler.toShort
import com.light672.zinc.lang.runtime.opcodes.*

class VirtualMachine(
	private val instance: com.light672.zinc.Zinc.Runtime,
	private val stack: Array<ZincValue?>,
	private val callStack: Array<CallFrame?>,
	private var stackSize: Int,
	private var callStackSize: Int,
	private var chunk: Chunk,
	private var pc: Int,
) {

	data class CallFrame(val bp: Int, val returnLocation: Int)

	constructor(instance: com.light672.zinc.Zinc.Runtime, stackMaxSize: Int, callStackMaxSize: Int, chunk: Chunk) : this(
		instance,
		arrayOfNulls<ZincValue>(stackMaxSize),
		arrayOfNulls<CallFrame>(callStackMaxSize),
		0,
		0,
		chunk,
		chunk.start,
	)

	private var stopQueued = false
	fun interpret() {
		reset()
		run()
	}

	fun reset() {
		pc = 0
		stackSize = 0
		callStackSize = 0
		stopQueued = false
		pushFirstFrame()
	}

	private fun run() {
		while (!stopQueued) {
			when (readByte()) {
				OP_CONST -> pushStack(chunk.constants[readShort().toInt()])
				OP_CREATE_NUM -> pushStack(ZincNumber(toShort(readByte(), readByte()).toDouble()))
				OP_ALLOC -> {
					val size = readByte().toInt()
					val array = Array(size) { i -> stack[stackSize - size + i] }
					stackSize - size
					pushStack(ZincGroup(array))
				}

				OP_NONE -> pushStack(null)
				OP_TRUE -> pushStack(ZincTrue)
				OP_FALSE -> pushStack(ZincFalse)
				OP_POP -> {
					instance.out.println(popStack().toString())
				}

				OP_ADD -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(a + b)
				}

				OP_SUB -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(a - b)
				}

				OP_DIV -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(a / b)
				}

				OP_MUL -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(a * b)
				}

				OP_MOD -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(a % b)
				}

				OP_POW -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(a.pow(b))
				}

				OP_NOT -> pushStack(ZincNumber(-((popStack() as ZincNumber).value)))

				OP_NEG -> pushStack(ZincBoolean(!((popStack() as ZincBoolean).value)))

				OP_JMP -> pc = toShort(readByte(), readByte()) + pc // do not replace with operator assignment

				OP_JIF -> {
					val short = toShort(readByte(), readByte())
					if (!(popStack() as ZincBoolean).value) pc += short
				}

				OP_JIT -> {
					val short = toShort(readByte(), readByte())
					if ((popStack() as ZincBoolean).value) pc += short
				}

				OP_GET_STACK -> pushStack(stack[bp + readByte()])
				OP_SET_STACK -> {
					stack[bp + readByte()] = peekStack()
				}

				OP_GET_IND -> pushStack((popStack() as ZincGroup).array[readByte().toInt()])
				OP_SET_IND -> {
					(popStack() as ZincGroup).array[readByte().toInt()] = peekStack()
				}

				OP_CREATE_FUNCTION -> {
					val arity = readByte().toInt()
					val captureSize = readByte().toInt()
					val captures = Array(captureSize) { i -> stack[stackSize - captureSize + i] }
					stackSize - captureSize
					pushStack(ZincFunction(arity, captures))
				}

				OP_CALL -> {
					var arity = readByte().toInt()
					pushCallFrame(arity)
					val function = popStack() as ZincFunction
					for (value in function.capturedArguments) {
						pushStack(value)
						arity++
					}
					pc = function.codeLocation
				}

				OP_RETURN -> {
					if (callStackSize == 1) return
					val frame = callStack[--callStackSize]!!
					pc = frame.returnLocation
					stackSize = frame.bp
				}

				OP_RETURN_VALUE -> {
					if (callStackSize == 1) return
					val returnValue = popStack()
					val frame = callStack[--callStackSize]!!
					pc = frame.returnLocation
					stackSize = frame.bp
					pushStack(returnValue)
				}

				OP_END -> {
					return
				}

				else -> TODO("not implemented")
			}
		}
	}

	private fun readByte() = chunk.code[pc++]
	private fun readShort() = toShort(readByte(), readByte())

	// private fun range() = chunk.ranges[pc]
	private val bp get() = callStack[callStackSize - 1]!!.bp

	private fun pushStack(value: ZincValue?) {
		if (stackSize > stack.size) throw StackOverflowError()
		stack[stackSize++] = value
	}

	private fun popStack() = if (stackSize == 0) throw StackOverflowError() else stack[--stackSize]!!
	private fun peekStack() = stack[stackSize - 1]
	private fun pushCallFrame(arity: Int) {
		if (callStackSize > callStack.size) throw StackOverflowError()
		callStack[callStackSize++] = CallFrame(stackSize - arity, pc)
	}

	private fun pushFirstFrame() {
		callStack[0] = CallFrame(0, 0)
		callStackSize++
	}

}