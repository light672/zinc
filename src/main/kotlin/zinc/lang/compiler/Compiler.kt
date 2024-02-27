package zinc.lang.compiler

import zinc.Zinc
import zinc.lang.compiler.parsing.Parser

internal class Compiler(val runtime: Zinc.Runtime, val source: String) {
	fun compile() {
		val (functions, variables) = Parser(source, runtime).parse()
		if (runtime.hadError) return
		val module = ZincModule(runtime, source, functions, variables)
		module.types["num"] = Type.Number
		module.types["str"] = Type.String
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