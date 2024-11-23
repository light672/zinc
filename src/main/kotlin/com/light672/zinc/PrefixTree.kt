package com.light672.zinc

internal class PrefixTree<T> {
	private val root = Node<T>()

	operator fun set(word: String, value: T) {
		var current = root
		for (c in word) {
			current.children.getOrPut(c) { Node() }
			current = current.children[c]!!
		}
		current.data = value
	}

	operator fun get(word: CharSequence): T? {
		var current = root
		for (c in word)
			current = current.children[c] ?: return null
		return current.data
	}

	class Node<T>(var data: T? = null) {
		val children = mutableMapOf<Char, Node<T>>()
	}
}