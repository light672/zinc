import com.light672.zinc.Zinc
import java.io.File
import java.nio.charset.Charset


fun main() {
	val script = File("src/main/kotlin/script.zc").readBytes().toString(Charset.defaultCharset())
	normalTest(script)
}

fun normalTest(source: String) {
	val runtime =
		Zinc.Runtime(
			256,
			256,
			source,
			System.out,
			System.err,
		)
	println(
		"${
			Zinc.time {
				runtime.run()
			}
		} ms"
	)
}