package com.light672.zinc.lang.compiler.parsing.syntax

internal class Path(val tail: Token, val body: List<Token>)
// ::? IDENTIFIER (:: IDENTIFIER)*