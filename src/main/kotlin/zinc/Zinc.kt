package zinc

import zinc.builtin.ZincException
import zinc.lang.compiler.Compiler
import zinc.lang.compiler.Lexer
import zinc.lang.compiler.Parser
import zinc.lang.compiler.Resolver
import zinc.lang.runtime.VirtualMachine

object Zinc {
	class Runtime internal constructor(
		private val stackSize: Int,
		private val callStackSize: Int,
		private val source: String,
		internal val out: OutputStream,
		private val err: OutputStream,
		internal val debug: Boolean
	) {
		private var hadError = false

		constructor(
			stackSize: Int,
			callStackSize: Int,
			source: String,
			out: OutputStream,
			err: OutputStream,
		) : this(stackSize, callStackSize, source, out, err, false)

		fun run() {
			if (debug) {
				val lexer = Lexer(source)
				println(lexer.scanTokens())
			}
			val parser = Parser(source, this)
			val statements = parser.parse()
			if (debug) println(statements)
			if (hadError) return
			val resolver = Resolver(this)
			resolver.resolve(statements)
			if (hadError) return
			val compiler = Compiler()
			val chunk = compiler.compile(statements)
			if (hadError) return
			val vm = VirtualMachine(this, stackSize, callStackSize, chunk)
			try {
				//vm.interpret()
			} catch (exception: ZincException) {
				reportRuntimeError(exception)
			}
		}

		private fun reportRuntimeError(error: ZincException) {
			err.print("${error.javaClass}: ${error.message}\n")
		}

		internal fun reportCompileError(message: String) {
			err.print("$message\n")
			hadError = true
		}


	}

	interface OutputStream {
		fun print(message: String)
	}

	object SystemOutputStream : OutputStream {
		override fun print(message: String) {
			kotlin.io.print(message)
		}
	}

	object SystemErrorStream : OutputStream {
		override fun print(message: String) {
			kotlin.io.print(message)
		}

	}
}