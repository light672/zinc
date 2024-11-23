package com.light672.zinc

import com.light672.zinc.ast.Parser
import com.light672.zinc.resolution.Resolver
import java.io.PrintStream

object Zinc {
	internal const val INDENT_SIZE = 4

	class Runtime internal constructor(
		private val stackSize: Int,
		private val callStackSize: Int,
		internal val source: String,
		internal val out: PrintStream,
		private val err: PrintStream,
	) {
		private var hadError = false
		private lateinit var lines: List<String>


		fun run() {
			val scope = ScopeInfo(this)
			Parser(this).parse(scope)
			Resolver(this).resolve(scope)
		}

		internal fun reportCompileError(error: CompilerError) {
			if (!this::lines.isInitialized)
				lines = source.replace("\t", "".padEnd(INDENT_SIZE)).split("\n")
			err.println("line ${error.lines.first}: ${error.message}")
			val padLength = error.lines.last.toString().length
			err.println(" | ".padStart(padLength + 3))
			for (line in error.lines) {
				err.println("${line.toString().padStart(padLength)} | ${lines[line - 1]}")
			}
			err.println(
				" | "
					.padStart(padLength + 3)
					.padEnd(padLength + 3 + error.range.first) + ""
					.padEnd(
						error.range.last - error.range.first,
						'^'
					)
			)
			err.println("error code ${error.code.ordinal}")
			err.println()
		}
	}


	internal inline fun time(block: () -> Unit): Long {
		val time = System.currentTimeMillis()
		block()
		return System.currentTimeMillis() - time
	}


}