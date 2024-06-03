package com.light672.zinc.lang.compiler.parsing.syntax.tools

sealed class Either<L, R> {
	data class Left<L, R>(val value: L) : Either<L, R>()
	data class Right<L, R>(val value: R) : Either<L, R>()
}