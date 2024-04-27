import com.light672.zinc.Zinc
import java.io.File
import java.nio.charset.Charset
import kotlin.concurrent.thread


fun main() {
	val benchmark = File("src/main/kotlin/benchmark.zc").readBytes().toString(Charset.defaultCharset())
	val script = File("src/main/kotlin/script.zc").readBytes().toString(Charset.defaultCharset())
	normalTest(script, true)
}

fun parserTimingTest(string: String) {
	val warmupString = """
		struct MyStruct {
			a: num
		}

		func main() {
			val init = MyStruct {a: 3};
			val a = init.a + add(init.a, 3);
		}

		func add(a: num, b: num): num {
			return a + b;
		}
	""".trimIndent()
	run {
		print("[PRATT] warmup: ")
		val warmupRuntime =
			Zinc.Runtime(256, 256, warmupString, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, false, Zinc.ParseType.PRATT)
		for (i in 0..<100000) {
			warmupRuntime.run()
			if (i % 1000 == 0)
				print("#")
		}
	}
	println()
	run {
		print("[REORDER] warmup: ")
		val warmupRuntime =
			Zinc.Runtime(256, 256, warmupString, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, false, Zinc.ParseType.REORDER)
		for (i in 0..<100000) {
			warmupRuntime.run()
			if (i % 1000 == 0)
				print("#")
		}
	}
	println()
	run {
		print("[RECURSIVE] warmup: ")
		val warmupRuntime =
			Zinc.Runtime(256, 256, warmupString, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, false, Zinc.ParseType.RECURSIVE)
		for (i in 0..<100000) {
			warmupRuntime.run()
			if (i % 1000 == 0)
				print("#")
		}
	}
	println()
	thread(true) {
		parserTest(string, true, Zinc.ParseType.REORDER)
	}
	thread(true) {
		parserTest(string, true, Zinc.ParseType.RECURSIVE)
	}
	thread(true) {
		parserTest(string, true, Zinc.ParseType.PRATT)
	}
}

fun parserTest(source: String, comprehensiveErrors: Boolean, type: Zinc.ParseType) {
	when (type) {
		Zinc.ParseType.PRATT -> {
			val prattRuntime =
				Zinc.Runtime(256, 256, source, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, Zinc.ParseType.PRATT)
			println("pratt parsing\ntime: ${Zinc.time { prattRuntime.run() }}ms")
		}

		Zinc.ParseType.RECURSIVE -> {
			val recursiveRuntime =
				Zinc.Runtime(256, 256, source, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, Zinc.ParseType.RECURSIVE)
			println("recursive parsing\ntime: ${Zinc.time { recursiveRuntime.run() }}ms")
		}

		Zinc.ParseType.REORDER -> {
			val reorderRuntime =
				Zinc.Runtime(256, 256, source, Zinc.SystemOutputStream, Zinc.SystemErrorStream, false, comprehensiveErrors, Zinc.ParseType.REORDER)
			println("reorder parsing\ntime: ${Zinc.time { reorderRuntime.run() }}ms")
		}
	}


}

fun normalTest(source: String, comprehensiveErrors: Boolean) {
	val runtime =
		Zinc.Runtime(
			256,
			256,
			source,
			Zinc.SystemOutputStream,
			Zinc.SystemErrorStream,
			false,
			comprehensiveErrors,
			Zinc.ParseType.PRATT
		)
	println(
		"${
			Zinc.time {
				runtime.run()
			}
		} ms"
	)
}