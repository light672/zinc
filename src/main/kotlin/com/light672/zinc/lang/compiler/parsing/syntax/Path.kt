package com.light672.zinc.lang.compiler.parsing.syntax

internal class Path(val tail: List<Token>, val head: Token)
// ::? IDENTIFIER (:: IDENTIFIER)*