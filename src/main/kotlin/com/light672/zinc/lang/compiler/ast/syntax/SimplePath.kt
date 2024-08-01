package com.light672.zinc.lang.compiler.ast.syntax

internal class SimplePath(val tail: List<Token>, val head: Token)
// ::? IDENTIFIER (:: IDENTIFIER)*