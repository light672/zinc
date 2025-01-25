package com.light672.zinc.resolution

import com.light672.zinc.analysis.MonoType
import com.light672.zinc.ast.Token
import com.light672.zinc.ast.Pattern as ASTPattern
import com.light672.zinc.ast.Stmt as ASTStmt


internal sealed interface Item

internal sealed interface AssociatedValue : ValueItem
internal sealed interface AssociatedType : TypeItem

internal sealed interface ValueItem : Item {
	class Function(val ast: ASTStmt) : AssociatedValue
	class Variable(val mutable: Boolean, val ast: ASTPattern) : ValueItem

	data object Ambiguous : ValueItem
}

internal sealed interface TypeItem : Item {
	class Struct(val ast: ASTStmt) : TypeItem {
		lateinit var getField: (name: String, generics: List<MonoType>) -> MonoType
	}

	class TupleStruct(val ast: ASTStmt.TupleStruct) : TypeItem
	class UnitStruct(val ast: ASTStmt.UnitStruct) : TypeItem
	class Module(val scope: Scope, val ast: ASTStmt.Module) : TypeItem
	class Generic(val index: Int, val ast: Token) : TypeItem
	class Primitive() : TypeItem
	data object Ambiguous : TypeItem
}