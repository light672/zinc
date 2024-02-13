package zinc

import zinc.builtin.ZincException
import zinc.lang.compiler.*

object Zinc {
	internal val defaultGlobalScope = Scope(null).also {
		// define primitives
		// do not define types like Unit and Nothing
		it.defineType("num", Type.Number)
		it.defineType("char", Type.Char)
		it.defineType("bool", Type.Bool)
		it.defineType("str", Type.String)
	}

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
			if (debug) println(Lexer(source).scanTokens())

			val statements = Parser(source, this).parse()

			if (debug) println(statements)

			if (hadError) return

			Resolver(this).resolve(statements)

			/*if (hadError) return
			val compiler = Compiler()
			val chunk = compiler.compile(statements)
			if (hadError) return
			val vm = VirtualMachine(this, stackSize, callStackSize, chunk)
			try {
				//vm.interpret()
			} catch (exception: ZincException) {
				reportRuntimeError(exception)
			}*/
		}

		private fun reportRuntimeError(error: ZincException) {
			err.print("${error.javaClass}: ${error.message}\n")
		}

		internal fun reportCompileError(message: String) {
			err.print("$message\n")
			hadError = true
		}


	}


	fun includeModule(module: ZincNativeModule) {

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
			System.err.println(message)
		}

	}
}