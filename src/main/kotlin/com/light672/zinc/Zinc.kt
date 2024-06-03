package com.light672.zinc

import com.light672.zinc.lang.compiler.parsing.Parser

object Zinc {


	class Runtime internal constructor(
		private val stackSize: Int,
		private val callStackSize: Int,
		private val source: String,
		internal val out: OutputStream,
		private val err: OutputStream,
	) {

		internal var hadError = false

		fun run() {
			Parser(source, this).parse()
		}

		private fun reportRuntimeError(error: String) {
			err.println("Panicked: $error")
		}

		internal fun reportCompileError(error: String) {
			err.println(error)
			hadError = true
		}


	}


	abstract class OutputStream {
		abstract fun print(message: String)
		fun println(message: String) = print(message + "\n")
	}

	object SystemOutputStream : OutputStream() {
		override fun print(message: String) {
			kotlin.io.print(message)
		}
	}

	object SystemErrorStream : OutputStream() {
		override fun print(message: String) {
			System.err.print(message)
		}
	}


	internal inline fun <T> time(block: () -> T): Long {
		val time = System.currentTimeMillis()
		block()
		return System.currentTimeMillis() - time
	}
}