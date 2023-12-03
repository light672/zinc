package zinc.lang.runtime

import zinc.builtin.ZincValue

data class Chunk(val code: Array<Byte>, val constants: Array<ZincValue>, val lines: Array<Int>)