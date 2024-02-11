import zinc.Zinc

fun main() {
	val runtime =
		Zinc.Runtime(256, 256, "val a = 6 + '3';", Zinc.SystemOutputStream, Zinc.SystemErrorStream, true)
	runtime.run()
}