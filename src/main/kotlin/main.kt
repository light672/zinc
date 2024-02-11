import zinc.Zinc

fun main() {
	val runtime =
		Zinc.Runtime(
			256, 256, """
			func add(a: num, b: num): str {
				val a = " ";
				a + b;
			}
		""".trimIndent(), Zinc.SystemOutputStream, Zinc.SystemErrorStream, true
		)
	runtime.run()
}