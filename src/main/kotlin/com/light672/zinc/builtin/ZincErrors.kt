package com.light672.zinc.builtin

open class ZincException(override val message: String) : RuntimeException(message)
class StackOverflowError(override val message: String) : ZincException(message) {
	constructor() : this("")
}