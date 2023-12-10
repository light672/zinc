package zinc.lang.runtime

import zinc.builtin.ZincBoolean
import zinc.builtin.ZincValue
import zinc.lang.Chunk

internal data class VirtualMachine(val chunk: Chunk, val maxStackSize: Int, val maxCallStackSize: Int) {

	data class CallFrame(val bp: Int, val pc: Int)

	private val stack = arrayOfNulls<ZincValue?>(maxStackSize)
	private var stackSize = 0

	private val callStack = arrayOfNulls<CallFrame>(maxCallStackSize)
	private var callStackSize = 0

	private var stopQueued = false

	// basic registers
	private var pc = 0
	private var bp = 0
	private val line get() = chunk.lines[pc]

	fun interpret() {
		// reset the vm
		pc = 0
		stackSize = 0
		callStackSize = 0
		stopQueued = false
		run()
	}

	private fun run() {
		while (!stopQueued) {
			when (readByte()) {
				OP_CONST -> pushStack(chunk.constants[readByte().toInt()])
				OP_TRUE -> pushStack(ZincBoolean(true))
				OP_FALSE -> pushStack(ZincBoolean(false))
				OP_NULL -> pushStack(null)
				OP_POP -> --stackSize

				OP_ADD -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a + b)
				}

				OP_SUB -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a - b)
				}

				OP_MUL -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a * b)
				}

				OP_DIV -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a / b)
				}

				OP_MOD -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a % b)
				}

				OP_POW -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a.pow(b))
				}

				OP_ADD_ASSIGN -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					a += b
				}

				OP_SUB_ASSIGN -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					a -= b
				}

				OP_MUL_ASSIGN -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					a *= b
				}

				OP_DIV_ASSIGN -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					a /= b
				}

				OP_MOD_ASSIGN -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					a %= b
				}

				OP_POW_ASSIGN -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					a.powAssign(b)
				}

				OP_CALL -> {
					val function = popStack() as Function
					pushFrame(function.arity)
					pc = function.pc
				}

				OP_RETURN -> {
					val value = popStack()
					val frame = popFrame()
					pc = frame.pc
					bp = frame.bp
					pushStack(value)
				}

				OP_END -> return
			}
		}
	}


	private fun readByte(): Byte = chunk.code[pc++]
	private fun readShort(): Short = ((chunk.code[pc++].toInt()) shl 8 or (chunk.code[pc++].toInt() and 0xFF)).toShort()
	private fun popStack(): ZincValue? = stack[--stackSize]
	private fun peekStack(): ZincValue? = stack[stackSize - 1]
	private fun pushStack(obj: ZincValue?) {
		stack[stackSize++] = obj
	}

	private fun popFrame(): CallFrame = callStack[--callStackSize]
		?: throw IllegalArgumentException("Unable to pop call stack because there are no call frames.")

	private fun pushFrame(arity: Int) {
		bp = stackSize - arity
		callStack[callStackSize++] = CallFrame(bp, pc)
	}

	/**
	 * Unsafe. Only use if you know what you are doing
	 * @param pc The new program counter
	 */
	fun moveProgramCounter(pc: Int) {
		this.pc = pc
	}


	private fun error(message: String) {
		// report error
		System.err.println("[line ${chunk.lines[pc]}] $message")
	}
}