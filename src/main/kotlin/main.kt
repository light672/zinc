import zinc.Zinc

fun main() {
	val runtime =
		Zinc.Runtime(
			256, 256, """
			val a: str = 3 * (11 + 6);
		""".trimIndent(), Zinc.SystemOutputStream, Zinc.SystemErrorStream, false
		)
	runtime.run()
}