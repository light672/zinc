package zinc.builtin.numbers

import kotlin.math.pow

fun Int.pow(b: Int): Int = this.toDouble().pow(b).toInt()