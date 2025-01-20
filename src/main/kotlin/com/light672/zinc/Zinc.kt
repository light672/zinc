package com.light672.zinc

import com.light672.zinc.ast.Parser
import com.light672.zinc.ast.TokenType
import com.light672.zinc.resolution.Resolver
import com.light672.zinc.resolution.Scope
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
			val parser = Parser(this)
			val stmtsResult = with(parser) { with(combinator) { expect(manyUntil(::declaration, TokenType.EOF)) } }
			if (!stmtsResult.isSuccess()) return
			val (stmts, eof) = +stmtsResult
			val scope = Scope()
			val resolver = Resolver(this)
			val irStmts = stmts.map { resolver.stmt(it, scope) }
			println(stmts)
		}

		internal fun reportCompileError(error: CompilerError) {
			val (start, end) = error.range
			if (!this::lines.isInitialized)
				lines = source.replace("\t", "".padEnd(INDENT_SIZE)).split("\n")
			err.println("line ${start.line}: ${error.message}")
			val padLength = end.toString().length
			err.println(" | ".padStart(padLength + 3))

			val lines = start.line..end.line
			val range = start.rangeOnLine.first.coerceAtLeast(end.rangeOnLine.first)..start.rangeOnLine.last.coerceAtLeast(end.rangeOnLine.last)

			for (line in lines) {
				err.println("${line.toString().padStart(padLength)} | ${this.lines[line - 1]}")
			}
			err.println(
				" | "
					.padStart(padLength + 3)
					.padEnd(padLength + 3 + range.first) + ""
					.padEnd(
						range.last - range.first,
						'^'
					)
					.let {
						if (error.secondaryLabel.isNotEmpty()) "$it - ${error.secondaryLabel}"
						else it
					}
			)
			if (error.primaryLabel.isEmpty()) {
				err.println()
				return
			}

			err.println(" | ".padStart(padLength + 3) + "|".padStart(range.first + 1))
			err.println(" | ".padStart(padLength + 3) + "".padStart(range.first) + error.primaryLabel)
		}
	}


	internal inline fun time(block: () -> Unit): Long {
		val time = System.currentTimeMillis()
		block()
		return System.currentTimeMillis() - time
	}


}