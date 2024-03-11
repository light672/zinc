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

	recvsprattTest(string, true)
}

fun recvsprattTest(source: String, comprehensiveErrors: Boolean) {


	println("pratt parsing\n")
	var string = source + " "
	var avg = 0L
	for (i in 0..100) {
		val prattRuntime = Zinc.Runtime(256, 256, string, Zinc.SystemOutputStream, Zinc.SystemErrorStream, true, comprehensiveErrors, true)
		avg += Zinc.time {
			prattRuntime.run()
		}
		string += " "
	}
	println(avg / 100)
	println("\nrecursive parsing\n")

	var string2 = source + " "
	var avg2 = 0L
	for (i in 0..100) {
		val recursiveRuntime = Zinc.Runtime(256, 256, string2, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, false)
		avg2 += Zinc.time {
			recursiveRuntime.run()
		}
		string2 += " "
	}
	println(avg2 / 100)
}