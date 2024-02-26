import zinc.Zinc


fun main() {
	val runtime =
		Zinc.Runtime(
			256, 256, """
			val a: str = " ";
			
			func main() {
				a = 3;
			}
			
			func add(a: num, b: num): num {
				return a + b;
			}
		""".trimIndent(), Zinc.SystemOutputStream, Zinc.SystemErrorStream, true
		)
	runtime.run()
}

