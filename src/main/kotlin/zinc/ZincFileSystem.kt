package zinc

class ZincDirectory private constructor(val name: String, val directory: ZincDirectory?) {
	internal val directories = HashMap<String, ZincDirectory>()
	internal val files = HashMap<String, ZincFile>()

	fun addFile(name: String, content: String): ZincDirectory {
		files[name] = ZincFile(name, content, this)
		return this
	}

	fun addDirectory(name: String, builder: (ZincDirectory) -> ZincDirectory): ZincDirectory {
		val dir = ZincDirectory(name, this)
		builder(dir)
		directories[name] = dir
		return this
	}

	companion object {
		fun base() = ZincDirectory("", null)
	}
}

class ZincFile internal constructor(internal val name: String, internal val content: String, internal val directory: ZincDirectory) {
	fun linesInRange(range: IntRange) = ArrayList<Triple<Int, String, IntRange>>().also {
		var len = 0
		for ((index, line) in content.lines().toTypedArray().withIndex()) {
			val previousLength = len
			len += line.length
			if (len > range.last) break
			if (len in range) it.add(Triple(index, line, previousLength..len))
		}
	}
}