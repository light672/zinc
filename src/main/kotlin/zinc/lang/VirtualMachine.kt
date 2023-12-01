package zinc.lang

import zinc.builtin.ZincBoolean
import zinc.builtin.ZincValue

internal data class VirtualMachine(val chunk: Chunk, val size: Int) {

	data class CallFrame(val bp: Int, val rpc: Int)

	private val stack = arrayOfNulls<ZincValue?>(size)
	private var stackSize = 0

	private val callStack = arrayOfNulls<CallFrame>(size / 2)
	private var callStackSize = 0

	private var stopQueued = false

	// basic registers
	private var pc = 0
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

	/**
	 * Unsafe. Only use if you know what you are doing
	 * @param arity The amount of
	 */
	fun pushFrame(arity: Int) {
		callStack[--callStackSize] = CallFrame(stackSize - arity, pc)
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