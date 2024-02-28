import zinc.Zinc
import java.io.File
import java.nio.charset.Charset


fun main() {
	val string = File("src/main/kotlin/script.zc").readBytes().toString(Charset.defaultCharset())
	val string2 = """
		func main() {
			val a: str = add(3, 2);
		}

		func add(a: num, b: num): num {
			return a + b;
		}
	""".trimIndent()
	println(
		"${
			Zinc.time {
				val runtime =
					Zinc.Runtime(
						256,
						256,
						string,
						Zinc.SystemOutputStream,
						Zinc.SystemErrorStream,
						false
					)
				runtime.run()
			}
		} ms"
	)
}

