package de.kiezpreis.core.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Tiny persistence layer: one JSON file per key, written atomically (temp file + rename),
 * so a crash during writing never leaves a half written file behind.
 */
class JsonFileStore(private val directory: File) {

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    init {
        directory.mkdirs()
    }

    fun <T> read(key: String, serializer: KSerializer<T>): T? {
        val file = fileFor(key)
        if (!file.exists()) return null
        return try {
            json.decodeFromString(serializer, file.readText())
        } catch (e: Exception) {
            // Corrupt or outdated file: keep a copy for diagnosis and start fresh.
            file.renameTo(File(directory, "$key.corrupt"))
            null
        }
    }

    @Synchronized
    fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        val target = fileFor(key)
        val tmp = File(directory, "$key.tmp")
        tmp.writeText(json.encodeToString(serializer, value))
        if (!tmp.renameTo(target)) {
            target.delete()
            check(tmp.renameTo(target)) { "Konnte $key nicht speichern" }
        }
    }

    fun delete(key: String) {
        fileFor(key).delete()
    }

    private fun fileFor(key: String): File {
        require(key.matches(Regex("[a-z0-9_]+"))) { "invalid key $key" }
        return File(directory, "$key.json")
    }
}
