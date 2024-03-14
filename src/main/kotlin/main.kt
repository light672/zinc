import com.light672.zinc.Zinc
import java.io.File
import java.nio.charset.Charset


fun main() {
	val string = File("src/main/kotlin/script.zc").readBytes().toString(Charset.defaultCharset())
	/*println(
		"${
			Zinc.time {
				val runtime =
					Zinc.Runtime(
						256,
						256,
						string,
						Zinc.SystemOutputStream,
						Zinc.SystemErrorStream,
						true,
						false,
						false
					)
				runtime.run()
			}
		} ms"
	)*/

	parserTest(string, true)
}

fun parserTest(source: String, comprehensiveErrors: Boolean) {
	val prattRuntime =
		Zinc.Runtime(256, 256, source, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, Zinc.ParseType.PRATT)
	val recursiveRuntime =
		Zinc.Runtime(256, 256, source, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, Zinc.ParseType.RECURSIVE)
	val reorderRuntime =
		Zinc.Runtime(256, 256, source, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, Zinc.ParseType.REORDER)
	run {
		println("pratt parsing")
		var avg = 0L
		for (i in 0..100) avg += Zinc.time { prattRuntime.run() }
		println("average: ${avg / 100}ms")
	}
	run {
		println("recursive parsing")
		var avg = 0L
		for (i in 0..100) avg += Zinc.time { recursiveRuntime.run() }
		println("average: ${avg / 100}ms")
	}
	run {
		println("reorder parsing")
		var avg = 0L
		for (i in 0..100) avg += Zinc.time { reorderRuntime.run() }
		println("average: ${avg / 100}ms")
	}

}