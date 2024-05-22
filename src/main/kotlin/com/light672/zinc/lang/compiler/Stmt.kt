package com.light672.zinc.lang.compiler

internal sealed class Stmt {
	class Expression(expr: Expr) : Stmt()
}