import zinc.Zinc

fun main() {
	val runtime = Zinc.Runtime(256, 256, "34 - 'e';", Zinc.SystemOutputStream, Zinc.SystemErrorStream, true)
	runtime.run()
}