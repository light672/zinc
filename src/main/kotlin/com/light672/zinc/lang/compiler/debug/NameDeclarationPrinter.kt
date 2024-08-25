package com.light672.zinc.lang.compiler.debug

import com.light672.zinc.lang.compiler.ir.*
import com.light672.zinc.lang.compiler.ir.Function

internal object NameDeclarationPrinter {
	fun print(namespace: Namespace) {
		printBranch("types", "", namespace.types)
		printBranch("values", "", namespace.values)
	}

	private fun printBranch(type: String, prefix: String, branch: Namespace.Branch) {
		println("$prefix$type:")

		for (item in branch) {
			val indent = if (item === branch.last()) INDENT_END else INDENT_MID
			when (item) {
				is Function -> {
					println(prefix + indent.first + "function ${item.name} (g-arity ${item.genericArity}):")
					item.types?.let { printBranch("types", prefix + indent.second + "    ", it) }
					item.values?.let { printBranch("values", prefix + indent.second + "    ", it) }
				}

				is Module -> {
					println(prefix + indent.first + "module ${item.name}:")
					printBranch("types", prefix + indent.second + indent.second, item.types)
					printBranch("values", prefix + indent.second + indent.second, item.values)
				}

				is TypeAlias -> println(prefix + indent.first + "typealias ${item.name} (g-arity ${item.genericArity})")
				is Struct -> println(prefix + indent.first + "struct ${item.name} (g-arity ${item.genericArity})")
				is Variable -> println(prefix + indent.first + "variable ${item.name} ${if (item.mutable) "(mutable) " else ""}")
			}
		}
	}
}