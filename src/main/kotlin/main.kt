import zinc.Zinc

fun main() {
	val runtime =
		Zinc.Runtime(
			256, 256, """
			func add(a: num, b: num): str {
				var a = 3;
				var b = "my string";
			}
		""".trimIndent(), Zinc.SystemOutputStream, Zinc.SystemErrorStream, true
		)
	runtime.run()
}