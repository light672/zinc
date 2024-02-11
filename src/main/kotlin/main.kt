import zinc.Zinc

fun main() {
	val runtime =
		Zinc.Runtime(
			256, 256, """
			func add(a: num, b: str): str {
				6 * 4 + ("3");
			}
		""".trimIndent(), Zinc.SystemOutputStream, Zinc.SystemErrorStream, true
		)
	runtime.run()
}