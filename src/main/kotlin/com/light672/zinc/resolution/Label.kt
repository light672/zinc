package com.light672.zinc.resolution

import com.light672.zinc.ast.Token

internal data class Label(val name: Token?, val expr: Expr?, val parent: Label?, val depthSinceItem: Int)