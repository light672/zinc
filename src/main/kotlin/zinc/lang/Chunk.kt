package zinc.lang

import zinc.builtin.ZincValue

class Chunk(val code: Array<Byte>, val constants: Array<ZincValue>, val lines: Array<Int>)