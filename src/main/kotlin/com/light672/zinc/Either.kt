package com.light672.zinc

internal sealed interface Either<out A, out B> {
	class Left<A>(value: A) : Either<A, Nothing>
	class Right<B>(value: B) : Either<Nothing, B>
}

internal fun <A, B> A?.or(b: B) = this?.let { Either.Left(this) } ?: Either.Right(b)