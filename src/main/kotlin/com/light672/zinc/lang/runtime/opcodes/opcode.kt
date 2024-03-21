package com.light672.zinc.lang.runtime.opcodes

internal const val OP_CONST /*[short]*/: Byte = 0
internal const val OP_CREATE_NUM /*[short]*/: Byte = 1
internal const val OP_ALLOC /*[byte]*/: Byte = 2
internal const val OP_NONE: Byte = 3
internal const val OP_TRUE: Byte = 4
internal const val OP_FALSE: Byte = 5
internal const val OP_POP: Byte = 6

internal const val OP_ADD: Byte = 7
internal const val OP_SUB: Byte = 8
internal const val OP_DIV: Byte = 9
internal const val OP_MUL: Byte = 10
internal const val OP_MOD: Byte = 11
internal const val OP_POW: Byte = 12

internal const val OP_NOT: Byte = 13
internal const val OP_NEG: Byte = 14

internal const val OP_JMP /*[short]*/: Byte = 15
internal const val OP_JIF /*[short]*/: Byte = 16
internal const val OP_JIT /*[short]*/: Byte = 17

internal const val OP_GET_STACK /*[byte]*/: Byte = 18
internal const val OP_SET_STACK /*[byte]*/: Byte = 19
internal const val OP_GET_IND: Byte = 20
internal const val OP_SET_IND: Byte = 21

internal const val OP_CREATE_FUNCTION /*[byte][byte]*/: Byte = 22

internal const val OP_CALL: Byte = 23
internal const val OP_CALL_NATIVE: Byte = 24
internal const val OP_RETURN: Byte = 25
internal const val OP_RETURN_VALUE: Byte = 26
internal const val OP_END: Byte = 27
