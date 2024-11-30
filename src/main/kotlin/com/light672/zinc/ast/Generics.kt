package com.light672.zinc.ast

internal class GenericParams(val params: List<Pair<Token, TypeParamBounds?>>)
internal class GenericArgs(val args: List<Type>)
internal class TypeParamBounds(val bounds: List<Type>)