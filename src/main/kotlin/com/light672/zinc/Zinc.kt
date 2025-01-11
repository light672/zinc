package com.light672.zinc

import com.light672.zinc.ast.Parser
import com.light672.zinc.ast.TokenType
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
			/*val combinator = CombinatorParser(this)

			val negativeParser = {
				with(combinator) {
					var negative: Token? = null
					optional(token(TokenType.MINUS))
						.with { negative = it }
						.then { token(TokenType.NUMBER) }
						.map { t -> parseInt(t.lexeme!!.toString()) * if (negative == null) 1 else -1 }
				}
			}

			val rangeParser = {
				with(combinator) {
					var first: Int? = null
					optional(negativeParser())
						.with { first = it }
						.then { token(TokenType.DOT_DOT) }
						.then { optional(negativeParser()) }
						.map { t -> (first ?: Int.MIN_VALUE)..(t ?: Int.MAX_VALUE) }
				}
			}

			println(rangeParser())*/

			println(with(Parser(this)) {
				with(combinator) {
					expect(manyUntil(::declaration, TokenType.EOF))
				}
			})
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