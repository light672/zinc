import zinc.Zinc

fun main() {
	val runtime =
		Zinc.Runtime(256, 256, "func a(a: num) {} val a = 15;", Zinc.SystemOutputStream, Zinc.SystemErrorStream, true)
	runtime.run()
}