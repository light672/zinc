package com.light672.zinc.lang.runtime.opcodes

internal const val OP_CONST /*[short]*/: Byte = 0
internal const val OP_CREATE_NUM /*[short]*/: Byte = 1
internal const val OP_ALLOC /*[size]*/: Byte = 2
internal const val OP_TRUE: Byte = 3
internal const val OP_FALSE: Byte = 4
internal const val OP_POP: Byte = 5

internal const val OP_ADD: Byte = 6
internal const val OP_SUB: Byte = 7
internal const val OP_DIV: Byte = 8
internal const val OP_MUL: Byte = 9
internal const val OP_MOD: Byte = 10
internal const val OP_POW: Byte = 11

internal const val OP_NOT: Byte = 12
internal const val OP_NEG: Byte = 13

internal const val OP_JMP: Byte = 14
internal const val OP_JIF: Byte = 15
internal const val OP_JIT: Byte = 16

internal const val OP_GET_STACK: Byte = 17
internal const val OP_SET_STACK: Byte = 18
internal const val OP_GET_IND: Byte = 19
internal const val OP_SET_IND: Byte = 20

internal const val OP_CREATE_FUNCTION /*[capturedVariableSize]*/: Byte = 21

internal const val OP_CALL: Byte = 22
internal const val OP_CALL_NATIVE: Byte = 23
internal const val OP_RETURN: Byte = 24
internal const val OP_END: Byte = 25
