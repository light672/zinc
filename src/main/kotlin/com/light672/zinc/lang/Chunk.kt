package com.light672.zinc.lang

import com.light672.zinc.builtin.ZincValue

class Chunk(val code: ByteArray, val constants: Array<ZincValue>, val ranges: Array<IntRange>)