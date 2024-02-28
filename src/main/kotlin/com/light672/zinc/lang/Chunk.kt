package com.light672.zinc.lang

import com.light672.zinc.builtin.ZincValue

class Chunk(val code: Array<Byte>, val constants: Array<ZincValue>, val lines: Array<Int>)