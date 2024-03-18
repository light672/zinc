package com.light672.zinc.lang.compiler

internal fun toBytes(int: Int) = Pair(((int shr 8) and 0xFF).toByte(), (int and 0xFF).toByte())
internal fun toInt(a: Byte, b: Byte) = (a.toInt() shl 8) or (b.toInt() and 0xFF)
internal fun toShort(a: Byte, b: Byte) = toInt(a, b).toShort()