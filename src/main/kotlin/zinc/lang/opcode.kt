package zinc.lang

val OP_CONSTANT: Byte = 0
val OP_TRUE: Byte = 1
val OP_FALSE: Byte = 2
val OP_NULL: Byte = 3


val OP_POP: Byte = 4


// may be null operations; slower
val OP_ADD: Byte = 5
val OP_SUBTRACT: Byte = 6
val OP_MULTIPLY: Byte = 7
val OP_DIVIDE: Byte = 8
val OP_MODULO: Byte = 9
val OP_EXPONENT: Byte = 10


// cannot be null operations; faster
val OP_ADD_UNSAFE: Byte = 11
val OP_SUBTRACT_UNSAFE: Byte = 12
val OP_MULTIPLY_NUMBER: Byte = 13
val OP_DIVIDE_NUMBER: Byte = 14
val OP_MODULO_NUMBER: Byte = 15
val OP_EXPONENT_NUMBER: Byte = 16


val OP_EQUAL: Byte = 17
val OP_NOT_EQUAL: Byte = 18

// may be null operations; slower
val OP_GREATER: Byte = 19
val OP_GREATER_EQUAL: Byte = 20
val OP_LESS: Byte = 21
val OP_LESS_EQUAL: Byte = 22


// cannot be null operations; faster
val OP_GREATER_UNSAFE: Byte = 23
val OP_GREATER_EQUAL_UNSAFE: Byte = 24
val OP_LESS_UNSAFE: Byte = 25
val OP_LESS_EQUAL_UNSAFE: Byte = 26

val OP_JUMP: Byte = 27
val OP_JUMP_IF_FALSE: Byte = 28

val OP_RETURN: Byte = 29

val OP_INTERRUPT: Byte = 30