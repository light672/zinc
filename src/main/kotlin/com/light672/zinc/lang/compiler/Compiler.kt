package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.lang.compiler.parsing.PrattParser
import com.light672.zinc.lang.compiler.parsing.RecursiveParser
import com.light672.zinc.lang.compiler.parsing.ReorderParser

internal class Compiler(val runtime: Zinc.Runtime, val source: String, val parseType: Zinc.ParseType) {
	fun compile() {
		val (structs, functions, variables) = when (parseType) {
			Zinc.ParseType.PRATT -> PrattParser(source, runtime).parse()
			Zinc.ParseType.RECURSIVE -> RecursiveParser(source, runtime).parse()
			Zinc.ParseType.REORDER -> ReorderParser(source, runtime).parse()
		}
		if (runtime.hadError) return
		val module = ZincModule(runtime, source, structs, functions, variables)
		module.types["num"] = Type.Number
		module.types["str"] = Type.String
		val structDeclarations = Array(structs.size) { i -> with(module.resolver) { structs[i].resolve() } }
		for ((i, struct) in structDeclarations.withIndex()) {
			struct ?: continue
			with(module.resolver) { struct.resolveStructInside(structs[i].fields) }
		}
		val functionDeclarations = Array(functions.size) { i -> with(module.resolver) { functions[i].resolve() } }
		if (runtime.hadError) return
		for (variable in variables) with(module.resolver) { variable.resolve() }
		if (runtime.hadError) return
		for (function in functionDeclarations) {
			function ?: continue
			with(module.resolver) { function.resolveFunctionBlock() }
		}
	}
}