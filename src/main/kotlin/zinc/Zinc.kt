package zinc

import zinc.builtin.ZincException
import zinc.lang.compiler.CompilerError
import zinc.lang.compiler.Lexer
import zinc.lang.compiler.Parser
import zinc.lang.compiler.Resolver

object Zinc {


	class Runtime internal constructor(
		private val stackSize: Int,
		private val callStackSize: Int,
		private val source: String,
		internal val out: OutputStream,
		private val err: OutputStream,
		internal val debug: Boolean,
	) {
		private var hadError = false

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
			err.println("${error.javaClass}: ${error.message}")
		}

		internal fun reportCompileError(error: CompilerError) {
			err.println(error.message)
			val linesInRange = ArrayList<Triple<Int, String, IntRange>>().also {
				var len = 0
				for ((index, line) in source.lines().toTypedArray().withIndex()) {
					val previousLength = len
					len += line.length
					if (len > error.range.last) break
					if (len in error.range) it.add(Triple(index, line, previousLength..len))
				}
			}

			when (error) {
				is CompilerError.TokenError -> {
					val (line, content, range) = linesInRange[0]
					val beginning = " |    ".padStart(line.toString().length)
					err.println(beginning)
					err.println("${error.token.line} |    $content")
					err.println(
						beginning + "".padStart(error.range.last - error.range.first, '^')
							.padStart(error.range.last - range.first) + error.message
					)
				}

				is CompilerError.OneRangeError -> {
					val last = linesInRange[linesInRange.size - 1]
					val numberSpace = last.first.toString().length
					val beginning = " |    ".padStart(numberSpace)
					err.println(beginning)


					for ((index, triple) in linesInRange.withIndex()) {
						val (line, content, _) = triple
						val lineString = String.format("% ${numberSpace}d", line)
						val char = if (linesInRange.size > 1) if (index == 0) '/' else '|' else ' '
						err.println("$lineString | $char  $content")
					}

					val toPad = if (linesInRange.size > 1) "^" else ""
					val padLength = error.range.last - error.range.first - if (linesInRange.size > 1) 1 else 0
					val padChar = if (linesInRange.size > 1) '_' else '^'
					err.println(
						beginning + toPad.padStart(padLength, padChar)
							.padStart(error.range.last - last.third.first)
								+ error.message
					)

				}
			}
			hadError = true
		}


	}


	fun includeModule(module: ZincNativeModule) {

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
			System.err.println(message)
		}
	}

	fun createZincRuntime() {

	}
}