package zinc.lang.runtime

import zinc.Zinc
import zinc.builtin.*
import zinc.lang.Chunk

class VirtualMachine(
	private val instance: Zinc.Runtime,
	private val stack: Array<ZincValue?>,
	private val callStack: Array<CallFrame?>,
	private var stackSize: Int,
	private var callStackSize: Int,
	private var chunk: Chunk,
	private var pc: Int,
) {

	data class CallFrame(val bp: Int, val returnLocation: Int)

	constructor(instance: Zinc.Runtime, stackMaxSize: Int, callStackMaxSize: Int, chunk: Chunk) : this(
		instance,
		arrayOfNulls<ZincValue>(stackMaxSize),
		arrayOfNulls<CallFrame>(callStackMaxSize),
		0,
		0,
		chunk,
		0,
	)

	private var stopQueued = false
	fun interpret() {
		reset()
		pushFirstFrame()
		run()
	}

	fun reset() {
		pc = 0
		stackSize = 0
		callStackSize = 0
		stopQueued = false
	}

	private fun run() {
		while (!stopQueued) {
			when (readByte()) {
				OP_CONST -> pushStack(chunk.constants[readByte().toInt()]) // change to readShort
				OP_TRUE -> pushStack(ZincTrue)
				OP_FALSE -> pushStack(ZincFalse)
				OP_POP -> {
					instance.out.print("${popStack()}\n")
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
					val returnValue = popStack()
					returnToPreviousFrame()
					pushStack(returnValue)
				}

				OP_END -> {
					return
				}
			}
		}
	}

	private fun readByte() = chunk.code[pc++]
	private fun line() = chunk.lines[pc]
	private fun bp() = callStack[callStackSize - 1]!!.bp

	private fun pushStack(value: ZincValue?) {
		if (stackSize > stack.size) throw StackOverflowError()
		stack[stackSize++] = value
	}

	private fun popStack() = if (stackSize == 0) throw StackOverflowError() else stack[--stackSize]!!

	private fun pushCallFrame(arity: Int) {
		if (callStackSize > callStack.size) throw StackOverflowError()
		callStack[callStackSize++] = CallFrame(stackSize - arity, pc)
	}

	private fun pushFirstFrame() {
		callStack[0] = CallFrame(0, 0)
	}

	private fun returnToPreviousFrame() {
		val frame = callStack[--callStackSize]!!
		pc = frame.returnLocation
		stackSize = frame.bp
	}
}