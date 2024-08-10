package com.light672.zinc

import com.light672.zinc.lang.compiler.ast.Parser
import com.light672.zinc.lang.compiler.ast.syntax.Stmt
import com.light672.zinc.lang.compiler.ast.syntax.Token
import com.light672.zinc.lang.compiler.debug.ASTPrinter
import com.light672.zinc.lang.compiler.debug.NameDeclarationPrinter
import com.light672.zinc.lang.compiler.ir.Namespace
import com.light672.zinc.lang.compiler.ir.Resolver

object Zinc {


	class Runtime internal constructor(
		private val stackSize: Int,
		private val callStackSize: Int,
		private val source: String,
		internal val out: OutputStream,
		private val err: OutputStream,
	) {

		private var hadError = false

		fun run() {
			val rootModuleStatement = Stmt.Module(Token.empty(), Token.empty(), ArrayList())
			val srcModuleStatement = Stmt.Module(Token.empty(), Token.newNA("src", 0, 0..0), ArrayList())

			val namespace = Namespace(this)

			val mainModuleStatement = Parser.parse(source, "main", namespace, this)
			ASTPrinter.print(mainModuleStatement)
			NameDeclarationPrinter.print(namespace)
			Resolver(namespace, this).resolveAndLower()
		}

		private fun reportRuntimeError(error: String) {
			err.println("Panicked: $error")
		}

		internal fun reportCompileError(error: String, range: Token.Range) {
			err.println("error at line ${range.first.line}: $error")
			val lines = source.split("\n") // do not change this to source.lines()
			val neededLines = Array(range.last.line - range.first.line + 1) { i -> lines[i + range.first.line - 1] }
			val padLength = range.last.line.toString().length
			err.println("".padStart(padLength) + " |")
			for ((i, line) in neededLines.withIndex()) err.println(
				"${
					(i + range.first.line).toString().padStart(padLength)
				} | $line"
			)
			err.println("".padStart(padLength) + " |")
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