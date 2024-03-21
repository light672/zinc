package com.light672.zinc.lang.compiler

import com.light672.zinc.Zinc
import com.light672.zinc.lang.Chunk
import com.light672.zinc.lang.compiler.parsing.PrattParser
import com.light672.zinc.lang.compiler.parsing.RecursiveParser
import com.light672.zinc.lang.compiler.parsing.ReorderParser

internal class Compiler(val runtime: Zinc.Runtime, val source: String, val parseType: Zinc.ParseType) {
	fun compile(): Chunk? {
		val (structs, functions, variables) = when (parseType) {
			Zinc.ParseType.PRATT -> PrattParser(source, runtime).parse()
			Zinc.ParseType.RECURSIVE -> RecursiveParser(source, runtime).parse()
			Zinc.ParseType.REORDER -> ReorderParser(source, runtime).parse()
		}
		if (runtime.hadError) return null
		val module = ZincModule(runtime, source, structs, functions, variables)
		val codeGenerator = CodeGenerator(runtime, module)
		module.globals.types["num"] = Type.Number
		module.globals.types["str"] = Type.String
		val structDeclarations = Array(structs.size) { i -> with(codeGenerator) { structs[i].resolve() } }
		for ((i, struct) in structDeclarations.withIndex()) {
			struct ?: continue
			with(codeGenerator) { struct.resolveStructInside(structs[i].fields) }
		}
		val functionDeclarations = Array(functions.size) { i -> with(codeGenerator) { functions[i].resolve() } }
		if (runtime.hadError) return null
		for (variable in variables) with(codeGenerator) { variable.resolve() }
		if (runtime.hadError) return null
		for (function in functionDeclarations) {
			function ?: continue
			with(codeGenerator) { function.resolveFunctionBlock() }
		}
		val byteArray = codeGenerator.code.toByteArray()
		val constantArray = codeGenerator.constants.toTypedArray()
		val main = module.globals.variables["main"]
		if (main == null || !main.first.function) {
			runtime.reportCompileError(CompilerError.SimpleError("Could not find main function."))
			return null
		}
		return Chunk(byteArray, constantArray, main.second)
	}
}