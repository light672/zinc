package com.light672.zinc.lang.compiler.ast.syntax

internal class GenericArgs(val open: Token, val args: List<Type>, val close: Token)
// '<' (Type ',')* Type ','? '>'