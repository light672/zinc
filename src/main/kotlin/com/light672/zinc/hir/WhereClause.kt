package com.light672.zinc.hir

internal data class WhereClause(val predicates: List<Pair<Type, TypeParamBounds>>)