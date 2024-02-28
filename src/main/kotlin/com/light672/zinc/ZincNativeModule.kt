package com.light672.zinc

abstract class ZincNativeModule {
	private val types = ArrayList<String>()

	fun defineStruct(name: String, fields: Array<Pair<String, String>>) {

	}

	fun defineFunction(name: String, parameters: Array<Pair<String, String>>) {

	}

	abstract fun getModuleName(): String
}