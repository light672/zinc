package zinc.builtin


open class ZincBoolean(override val truthy: Boolean) : ZincValue()
object ZincFalse : ZincBoolean(false)
object ZincTrue : ZincBoolean(true)