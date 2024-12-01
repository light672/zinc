package com.light672.zinc.tuple

internal interface ExpandableTuple

internal typealias Tuple2<T1, T2> = Pair<T1, T2>
internal typealias Tuple3<T1, T2, T3> = Triple<T1, T2, T3>

internal data class Tuple4<T1, T2, T3, T4>(val f1: T1, val f2: T2, val f3: T3, val f4: T4)
internal data class Tuple5<T1, T2, T3, T4, T5>(val f1: T1, val f2: T2, val f3: T3, val f4: T4, val f5: T5)
internal data class Tuple6<T1, T2, T3, T4, T5, T6>(val f1: T1, val f2: T2, val f3: T3, val f4: T4, val f5: T5, val f6: T6)
internal data class Tuple7<T1, T2, T3, T4, T5, T6, T7>(val f1: T1, val f2: T2, val f3: T3, val f4: T4, val f5: T5, val f6: T6, val f7: T7)
internal data class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>(
	val f1: T1,
	val f2: T2,
	val f3: T3,
	val f4: T4,
	val f5: T5,
	val f6: T6,
	val f7: T7,
	val f8: T8
)

internal data class Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(
	val f1: T1,
	val f2: T2,
	val f3: T3,
	val f4: T4,
	val f5: T5,
	val f6: T6,
	val f7: T7,
	val f8: T8,
	val f9: T9
)

internal data class Tuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(
	val f1: T1,
	val f2: T2,
	val f3: T3,
	val f4: T4,
	val f5: T5,
	val f6: T6,
	val f7: T7,
	val f8: T8,
	val f9: T9,
	val f10: T10
)

internal fun <T1, T2, T3> Tuple2<T1, T2>.expand(f3: T3) = Tuple3(first, second, f3)
internal fun <T1, T2, T3, T4> Tuple3<T1, T2, T3>.expand(f4: T4) = Tuple4(first, second, third, f4)
internal fun <T1, T2, T3, T4, T5> Tuple4<T1, T2, T3, T4>.expand(f5: T5) = Tuple5(f1, f2, f3, f4, f5)
internal fun <T1, T2, T3, T4, T5, T6> Tuple5<T1, T2, T3, T4, T5>.expand(f6: T6) = Tuple6(f1, f2, f3, f4, f5, f6)
internal fun <T1, T2, T3, T4, T5, T6, T7> Tuple6<T1, T2, T3, T4, T5, T6>.expand(f7: T7) = Tuple7(f1, f2, f3, f4, f5, f6, f7)
internal fun <T1, T2, T3, T4, T5, T6, T7, T8> Tuple7<T1, T2, T3, T4, T5, T6, T7>.expand(f8: T8) = Tuple8(f1, f2, f3, f4, f5, f6, f7, f8)
internal fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>.expand(f9: T9) = Tuple9(f1, f2, f3, f4, f5, f6, f7, f8, f9)
internal fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>.expand(f10: T10) =
	Tuple10(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10)