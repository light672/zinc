package zinc.lang

import zinc.builtin.ValueError
import zinc.builtin.ZincBoolean
import zinc.builtin.ZincNumber
import zinc.builtin.ZincValue

internal data class VirtualMachine(val chunk: Chunk, val size: Int) {

	private val stack = arrayOfNulls<ZincValue?>(size)
	private var stackSize = 0

	private val globals = arrayOfNulls<ZincValue?>(size)

	private var stopQueued = false

	// basic registers
	private var pc = 0
	private var bp = 0
	private val line get() = chunk.lines[pc]

	fun interpret() {
		// reset the vm
		pc = 0
		bp = 0
		stackSize = 0
		stopQueued = false

		run()
	}

	private fun run() {
		while (!stopQueued) {
			when (readByte()) {
				OP_CONSTANT -> pushStack(chunk.constants[readByte().toInt()])
				OP_TRUE -> pushStack(ZincBoolean(true))
				OP_FALSE -> pushStack(ZincBoolean(false))
				OP_NULL -> pushStack(null)
				OP_POP -> --stackSize
				OP_ADD -> {
					val b = popStack()
					val a = popStack()
					a?.let { b?.let { pushStack(a + b) } }
						?: throw ValueError(
							"Unsupported operand type(s) for '+': ${a?.javaClass ?: "null"} and ${b?.javaClass ?: "null"}."
						)
				}

				OP_SUBTRACT -> {
					val b = popStack()
					val a = popStack()
					a?.let { b?.let { pushStack(a - b) } }
						?: throw ValueError(
							"Unsupported operand type(s) for '-': ${a?.javaClass ?: "null"} and ${b?.javaClass ?: "null"}."
						)
				}

				OP_MULTIPLY -> {
					val b = popStack()
					val a = popStack()
					a?.let { b?.let { pushStack(a - b) } }
						?: throw ValueError(
							"Unsupported operand type(s) for '+': ${a?.name ?: "null"} and ${b?.name ?: "null"}."
						)
				}

				OP_DIVIDE -> {
					val b = popStack()
					val a = popStack()
					a?.let { b?.let { pushStack(a / b) } }
						?: throw ValueError(
							"Unsupported operand type(s) for '/': ${a?.name ?: "null"} and ${b?.name ?: "null"}."
						)
				}

				OP_MODULO -> {
					val b = popStack()
					val a = popStack()
					a?.let { b?.let { pushStack(a % b) } }
						?: throw ValueError(
							"Unsupported operand type(s) for '/': ${a?.name ?: "null"} and ${b?.name ?: "null"}."
						)
				}

				OP_EXPONENT -> {
					val b = popStack()
					val a = popStack()
					a?.let { b?.let { pushStack(a.pow(b)) } }
						?: throw ValueError(
							"Unsupported operand type(s) for '/': ${a?.name ?: "null"} and ${b?.name ?: "null"}."
						)
				}

				OP_ADD_UNSAFE -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a + b)
				}

				OP_SUBTRACT_UNSAFE -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a - b)
				}

				OP_MULTIPLY_NUMBER -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a + b)
				}

				OP_DIVIDE_NUMBER -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a / b)
				}

				OP_MODULO_NUMBER -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a % b)
				}

				OP_EXPONENT_NUMBER -> {
					val b = popStack() as ZincValue
					val a = popStack() as ZincValue
					pushStack(a.pow(b))
				}

				OP_EQUAL -> {
					val b = popStack()
					val a = popStack()
					pushStack(ZincBoolean(a == b))
				}

				OP_NOT_EQUAL -> {
					val b = popStack()
					val a = popStack()
					pushStack(ZincBoolean(a != b))
				}

				OP_GREATER -> {
					val b = popStack()
					val a = popStack()
					if (a is ZincNumber && b is ZincNumber)
						pushStack(ZincBoolean(a.value > b.value))
					else
						throw ValueError("Unsupported operand type(s) for '>': ${a?.name ?: "null"} and ${b?.name ?: "null"}.")
				}

				OP_GREATER_EQUAL -> {
					val b = popStack()
					val a = popStack()
					if (a is ZincNumber && b is ZincNumber)
						pushStack(ZincBoolean(a.value >= b.value))
					else
						throw ValueError("Unsupported operand type(s) for '>=': ${a?.name ?: "null"} and ${b?.name ?: "null"}.")
				}

				OP_LESS -> {
					val b = popStack()
					val a = popStack()
					if (a is ZincNumber && b is ZincNumber)
						pushStack(ZincBoolean(a.value < b.value))
					else
						throw ValueError("Unsupported operand type(s) for '<': ${a?.name ?: "null"} and ${b?.name ?: "null"}.")
				}

				OP_LESS_EQUAL -> {
					val b = popStack()
					val a = popStack()
					if (a is ZincNumber && b is ZincNumber)
						pushStack(ZincBoolean(a.value < b.value))
					else
						throw ValueError("Unsupported operand type(s) for '>=': ${a?.name ?: "null"} and ${b?.name ?: "null"}.")
				}

				OP_GREATER_UNSAFE -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(ZincBoolean(a.value > b.value))
				}

				OP_GREATER_EQUAL_UNSAFE -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(ZincBoolean(a.value >= b.value))
				}

				OP_LESS_UNSAFE -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(ZincBoolean(a.value < b.value))
				}

				OP_LESS_EQUAL_UNSAFE -> {
					val b = popStack() as ZincNumber
					val a = popStack() as ZincNumber
					pushStack(ZincBoolean(a.value <= b.value))
				}

				OP_JUMP -> {
					val jump = readShort()
					pc += jump
				}

				OP_JUMP_IF_FALSE -> {
					val jump = readShort()
					peekStack()?.let { x -> if (!x.truthy) pc += jump }
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

	private fun error(message: String) {
		// report error
		System.err.println("[line ${chunk.lines[pc]}] $message")
	}
}