package com.light672.zinc.lang.compiler

import com.light672.zinc.lang.compiler.parsing.Parser
import com.light672.zinc.lang.compiler.parsing.PrattParser

internal class Compiler(val runtime: com.light672.zinc.Zinc.Runtime, val source: String, val prattParsing: Boolean) {
	fun compile() {
		val (structs, functions, variables) = if (prattParsing) PrattParser(source, runtime).parse() else Parser(source, runtime).parse()
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