package pl.vitaltrack.core.testing

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Test

object Assert {
    fun assertEquals(e: Any?, a: Any?, m: String = "") { if (e != a) throw AssertionError("$m exp <$e> act <$a>") }
    fun assertEquals(e: Int, a: Int, m: String = "") { if (e != a) throw AssertionError("$m exp <$e> act <$a>") }
    fun assertEquals(e: Long, a: Long, m: String = "") { if (e != a) throw AssertionError("$m exp <$e> act <$a>") }
    fun assertTrue(c: Boolean, m: String = "") { if (!c) throw AssertionError("assertTrue: $m") }
    fun assertFalse(c: Boolean, m: String = "") { if (c) throw AssertionError("assertFalse: $m") }
    fun assertNull(v: Any?, m: String = "") { if (v != null) throw AssertionError("assertNull: $m") }
    fun assertNotNull(v: Any?, m: String = ""): Any { if (v == null) throw AssertionError("assertNotNull: $m"); return v }
    fun assertArrayEquals(e: ByteArray, a: ByteArray, m: String = "") {
        if (!e.contentEquals(a)) throw AssertionError("$m arrays differ")
    }
}

object TestRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        var pass = 0; var fail = 0
        for (name in args) {
            val cls = Class.forName(name)
            val inst = cls.getDeclaredConstructor().newInstance()
            for (m in cls.methods.sortedBy { it.name }) {
                if (!m.isAnnotationPresent(Test::class.java)) continue
                try { m.invoke(inst); pass++; println("PASS  ${cls.simpleName}.${m.name}") }
                catch (e: Throwable) { fail++; val c = e.cause ?: e; println("FAIL  ${cls.simpleName}.${m.name} -> $c") }
            }
        }
        println()
        println("Tests: $pass passed, $fail failed, ${args.size} classes")
        kotlin.system.exitProcess(if (fail == 0) 0 else 1)
    }
}
