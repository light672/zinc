package com.light672.zinc.dsr

import com.light672.zinc.ast.FunctionParam
import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Pattern as ASTPattern
import com.light672.zinc.ast.Stmt as ASTStmt

internal sealed interface ValueItem
internal sealed interface TypeItem {
	fun name() = when (this) {
		is Generic     -> "generic"
		is Module      -> "module"
		is Struct      -> "struct"
		is Trait       -> "trait"
		is TupleStruct -> "tuple struct"
		is UnitStruct  -> "unit struct"
	}
}

internal data class Generic(val index: Int, val ast: Token) : TypeItem
internal data class Module(val ast: ASTStmt.Module, val values: Branch<ValueItem>, val types: Branch<TypeItem>) : TypeItem
internal data class Trait(val ast: ASTStmt.Trait) : TypeItem
internal data class Struct(val ast: ASTStmt.Struct) : TypeItem
internal data class UnitStruct(val ast: ASTStmt.UnitStruct) : TypeItem, ValueItem
internal data class TupleStruct(val ast: ASTStmt.TupleStruct) : TypeItem
internal data class Function(val ast: ASTStmt.Function) : ValueItem
internal data class Variable(val ast: ASTPattern) : ValueItem
internal data class Self(val ast: FunctionParam.Self) : ValueItem
