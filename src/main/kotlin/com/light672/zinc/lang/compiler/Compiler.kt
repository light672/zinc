package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.lang.Chunk
import com.light672.zinc.lang.compiler.parsing.PrattParser
import com.light672.zinc.lang.compiler.parsing.RecursiveParser
import com.light672.zinc.lang.compiler.parsing.ReorderParser

internal class Compiler(val runtime: Zinc.Runtime, val source: String, val parseType: Zinc.ParseType) {
	fun compile(): Chunk? {
		val parser = when (parseType) {
			Zinc.ParseType.PRATT -> PrattParser(source, runtime)
			Zinc.ParseType.RECURSIVE -> RecursiveParser(source, runtime)
			Zinc.ParseType.REORDER -> ReorderParser(source, runtime)
		}
		val (structs, functions, variables) = parser.parse()
		val module = ZincModule(runtime, source, structs, functions, variables)
		val resolver = Resolver(runtime, module, true)
		module.globals.types["num"] = Type.Number
		module.globals.types["char"] = Type.Char
		module.globals.types["bool"] = Type.Bool
		module.globals.types["str"] = Type.String
		resolver.resolve()
		return null
	}
}