package pl.vitaltrack.core.common

import java.util.concurrent.CopyOnWriteArrayList

/** Obserwowalny stan (StateFlow-substytut) — UI nie trzyma własnej prawdy. */
class Obs<T>(initial: T) {
    @Volatile var value: T = initial; private set
    private val ls = CopyOnWriteArrayList<(T) -> Unit>()

    fun set(v: T) { synchronized(this) { value = v }; fire() }
    fun update(f: (T) -> T) { synchronized(this) { value = f(value) }; fire() }
    fun observe(l: (T) -> Unit): () -> Unit { ls.add(l); l(value); return { ls.remove(l) } }
    private fun fire() { val v = value; for (l in ls) try { l(v) } catch (ignored: Throwable) {} }
}
