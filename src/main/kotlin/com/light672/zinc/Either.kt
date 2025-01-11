package com.light672.zinc

internal sealed interface Either<out A, out B> {
	class Left<A>(val value: A) : Either<A, Nothing>
	class Right<B>(val value: B) : Either<Nothing, B>
	companion object {
		fun <A, B> or(a: A?, b: B) = a?.let { Left(a) } ?: Right(b)
	}
}