package com.light672.zinc.lang.compiler.parsing.syntax

internal class GenericArgs(val open: Token, val args: List<Type>, val close: Token)
// '<' (Type ',')* Type ','? '>'