package pl.vitaltrack

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import java.util.HashMap

/** Minimalny FileProvider (bez androidx) — pliki eksportu. */
class AppFileProvider : ContentProvider() {

    private var root: File? = null

    override fun onCreate(): Boolean {
        val c = context ?: return false
        root = File(c.getExternalFilesDir(null), "export").apply { mkdirs() }
        return true
    }

    private fun file(uri: Uri): File? {
        val name = uri.lastPathSegment ?: return null
        val r = root ?: return null
        val f = File(r, name)
        return if (f.canonicalPath.startsWith(r.absolutePath)) f else null
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = file(uri)
        if (f == null || !f.exists()) throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String {
        val f = file(uri) ?: return "application/octet-stream"
        val ext = f.name.substringAfterLast('.', "").lowercase()
        return MIME[ext] ?: "application/octet-stream"
    }

    override fun query(uri: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
    override fun insert(uri: Uri, v: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?): Int = 0

    companion object {
        private val MIME = HashMap<String, String>().apply {
            put("csv", "text/csv"); put("json", "application/json")
            put("gpx", "application/gpx+xml"); put("txt", "text/plain")
        }
    }
}
