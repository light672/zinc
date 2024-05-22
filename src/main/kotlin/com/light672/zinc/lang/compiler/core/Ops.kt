package com.light672.zinc.lang.compiler.core

import com.light672.zinc.lang.compiler.constructs.Scope
import com.light672.zinc.lang.compiler.constructs.Trait

internal object Ops {
	val namespace = Scope().also {
		it.traits["Add"] = Trait()
		it.traits["Sub"] = Trait()
		it.traits["Mul"] = Trait()
		it.traits["Div"] = Trait()
		it.traits["Rem"] = Trait()
		it.traits["Pow"] = Trait()
	}
}