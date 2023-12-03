package zinc.lang

import zinc.builtin.ZincValue

data class Function(val pc: Int, val arity: Int) : ZincValue()