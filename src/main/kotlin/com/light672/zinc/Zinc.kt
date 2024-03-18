package com.light672.zinc

import com.light672.zinc.lang.compiler.Compiler
import com.light672.zinc.lang.compiler.CompilerError
import com.light672.zinc.lang.compiler.parsing.Lexer
import kotlin.math.max

object Zinc {

	enum class ParseType {
		PRATT,
		RECURSIVE,
		REORDER
	}

	class Runtime internal constructor(
		private val stackSize: Int,
		private val callStackSize: Int,
		private val source: String,
		internal val out: OutputStream,
		private val err: OutputStream,
		internal val debug: Boolean,
		internal val comprehensiveErrors: Boolean,
		internal val parseType: ParseType
	) {

		internal var hadError = false

		fun run() {
			if (debug) println(Lexer(source).scanTokens())
			Compiler(this, source, parseType).compile()
		}

		private fun reportRuntimeError(error: String) {
			err.println("Panicked: $error")
		}

		internal fun reportCompileError(error: CompilerError) {
			err.println(error.message)
			if (!comprehensiveErrors) return
			fun linesInRange(range: IntRange) = ArrayList<Triple<Int, String, IntRange>>().also {
				var len = 0
				val lines = source.replace("\t", "    ").split("\n")
				for ((index, line) in lines.withIndex()) {
					val prevLen = len
					len += line.length + if (index == lines.size - 1) 0 else 1
					val lineRange = prevLen..<len
					if (range.first in lineRange || range.last in lineRange) {
						it.add(Triple(index, line, lineRange))
					}
				}
			}

			when (error) {
				is CompilerError.TokenError -> {
					val (_, content, range) = linesInRange(error.token.range)[0]

					val lineNumLen = error.token.line.toString().length

					val beginning = " |    ".leftPad(lineNumLen)
					err.println(beginning)
					err.println("${error.token.line} |    $content")
					err.print(beginning)
					err.print(' ' * (error.token.range.first - range.first))
					err.print('^' * error.token.range.len())
					err.println(" ${error.message}")
				}

				is CompilerError.OneRangeError -> {
					val linesInRange = linesInRange(error.range)
					val lineNumLen = linesInRange.last().first.toString().length
					val beginning = " |    ".leftPad(lineNumLen)
					err.println(beginning)


					for ((index, triple) in linesInRange.withIndex()) {
						val (l, content, _) = triple
						val line = l + 1
						val lineString = String.format("%${lineNumLen}d", line)
						val char = if (linesInRange.size > 1) if (index == 0) '/' else '|' else ' '
						err.println("$lineString | $char  $content")
					}

					err.print(beginning)
					if (linesInRange.size > 1) {
						err.print(' ' * (error.range.last - linesInRange.last().third.last))
						err.print('_' * (error.range.len() - linesInRange.last().third.len() + 1))
						err.print("^ ${error.message}")
					} else {
						val line = linesInRange[0]
						err.print(' ' * (error.range.first - line.third.first))
						err.print('^' * error.range.len())
						err.println(" ${error.message}")
					}
				}

				is CompilerError.TwoRangeError -> {
					val linesInFirstRange = linesInRange(error.rangeA)
					val linesInSecondRange = linesInRange(error.rangeB)
					val lineNumLen = max(linesInFirstRange.last().first, linesInSecondRange.last().first).toString().length
					val beginning = " |    ".leftPad(lineNumLen)
					err.println(beginning)

					for ((index, triple) in linesInFirstRange.withIndex()) {
						val (l, content, _) = triple
						val line = l + 1
						val lineString = String.format("%${lineNumLen}d", line)
						val char = if (linesInFirstRange.size > 1) if (index == 0) '/' else '|' else ' '
						err.println("$lineString | $char  $content")
					}

					err.print(beginning)
					if (linesInFirstRange.size > 1) {
						err.print(' ' * (error.rangeA.last - linesInFirstRange.last().third.last))
						err.print('_' * (error.rangeA.len() - linesInFirstRange.last().third.len() + 1))
						err.print("^ ${error.rangeAMessage}")
					} else {
						val line = linesInFirstRange[0]
						err.print(' ' * (error.rangeA.first - line.third.first))
						err.print('^' * error.rangeA.len())
						err.println(" ${error.rangeAMessage}")
					}

					for ((index, triple) in linesInSecondRange.withIndex()) {
						val (l, content, _) = triple
						val line = l + 1
						val lineString = String.format("%${lineNumLen}d", line)
						val char = if (linesInSecondRange.size > 1) if (index == 0) '/' else '|' else ' '
						err.println("$lineString | $char  $content")
					}

					err.print(beginning)
					if (linesInSecondRange.size > 1) {
						err.print(' ' * (error.rangeB.last - linesInSecondRange.last().third.last))
						err.print('_' * (error.rangeB.len() - linesInSecondRange.last().third.len() + 1))
						err.print("^ ${error.rangeBMessage}")
					} else {
						val line = linesInSecondRange[0]
						err.print(' ' * (error.rangeB.first - line.third.first))
						err.print('^' * error.rangeB.len())
						err.println(" ${error.rangeBMessage}")
					}
				}
			}
			err.println("")
			hadError = true
		}


	}


	fun includeModule(module: com.light672.zinc.ZincNativeModule) {

	}


	abstract class OutputStream {
		abstract fun print(message: String)
		fun println(message: String) = print(message + "\n")
	}

	object SystemOutputStream : com.light672.zinc.Zinc.OutputStream() {
		override fun print(message: String) {
			kotlin.io.print(message)
		}
	}

	object SystemErrorStream : com.light672.zinc.Zinc.OutputStream() {
		override fun print(message: String) {
			System.err.print(message)
		}
	}

	private fun String.leftPad(amount: Int, char: Char = ' ') = padStart(length + amount, char)
	private operator fun Char.times(a: Int) = "".leftPad(a, this)
	private fun IntRange.firstToZero() = 0..last - first
	private fun IntRange.len() = firstToZero().last


	internal inline fun <T> time(block: () -> T): Long {
		val time = System.currentTimeMillis()
		block()
		return System.currentTimeMillis() - time
	}
}