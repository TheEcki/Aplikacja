package pl.vitaltrack.core.analytics

/** Analityka bez wartości pomiarów (no-op w tym buildzie). */
interface Analytics { fun event(name: String, params: Map<String, String> = emptyMap()) }

object NoOp : Analytics {
    override fun event(name: String, params: Map<String, String>) { /* dane zostają na telefonie */ }
}
