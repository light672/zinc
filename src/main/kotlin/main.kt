import com.light672.zinc.Zinc
import com.light672.zinc.lang.compiler.parsing.PrattParser
import com.light672.zinc.lang.compiler.parsing.RecursiveParser
import com.light672.zinc.lang.compiler.parsing.ReorderParser
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
	allEqualTest(string, true)
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

fun allEqualTest(source: String, comprehensiveErrors: Boolean) {
	val runtime = Zinc.Runtime(256, 256, source, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, Zinc.ParseType.PRATT)
	val pratt = PrattParser(source, runtime).parse()
	val recursive = RecursiveParser(source, runtime).parse()
	val reorder = ReorderParser(source, runtime).parse()
	for (i in 0..<pratt.second.size) {
		val pf = pratt.second[i]
		val rf = reorder.second[i]
		println(pf.body.toList())
		println(rf.body.toList())
	}
	println("pratt == recursive: ${pratt.first == recursive.first && pratt.second == recursive.second && pratt.third == recursive.third}")
	println("pratt == reorder: ${pratt.first == reorder.first && pratt.second == reorder.second && pratt.third == reorder.third}")
}