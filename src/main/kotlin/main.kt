import java.io.File
import java.nio.charset.Charset


fun main() {
	val string = File("src/main/kotlin/script.zc").readBytes().toString(Charset.defaultCharset())
	println(
		"${
			com.light672.zinc.Zinc.time {
				val runtime =
					com.light672.zinc.Zinc.Runtime(
						256,
						256,
						string,
						com.light672.zinc.Zinc.SystemOutputStream,
						com.light672.zinc.Zinc.SystemErrorStream,
						false
					)
				runtime.run()
			}
		} ms"
	)
}

