package com.light672.zinc.builtin

class ZincFunction(val codeLocation: Int, val capturedArguments: Array<ZincValue?>) : ZincValue() {
	override val name = "Function"
}