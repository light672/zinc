package com.light672.zinc.lang.compiler.ir

internal class Static(name: String, mutable: Boolean, val letBinding: IRStmt.LetBinding) : Variable(name, mutable)