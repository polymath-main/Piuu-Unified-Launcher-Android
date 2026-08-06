package com.piuu.launcher.marketplace

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Piuu Extension Package Handler (.piuu bundle archive extractor and installer)
 */
object PiuuExtensionPackage {

    private const val TAG = "PiuuExtensionPackage"
    private const val MANIFEST_FILENAME = "plugin.json"

    /**
     * Unpack and install a .piuu bundle archive file into local SDK sandbox.
     */
    fun installBundleArchive(context: Context, zipFile: File): Pair<Boolean, String> {
        if (!zipFile.exists() || !zipFile.canRead()) {
            return Pair(false, "Invalid archive path: File does not exist or is not readable.")
        }

        val sandboxDir = File(context.filesDir, "custom_plugins")
        if (!sandboxDir.exists()) sandboxDir.mkdirs()

        var manifestJsonStr: String? = null
        val pluginId = "piuu_bundle_" + zipFile.nameWithoutExtension.lowercase().replace("[^a-z0-9_]".toRegex(), "_")
        val extractTargetDir = File(sandboxDir, pluginId)
        if (!extractTargetDir.exists()) extractTargetDir.mkdirs()

        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(8192)

                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory) {
                        val destinationFile = File(extractTargetDir, entryName)
                        val parent = destinationFile.parentFile
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs()
                        }

                        FileOutputStream(destinationFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }

                        if (entryName.equals(MANIFEST_FILENAME, ignoreCase = true)) {
                            manifestJsonStr = destinationFile.readText()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (manifestJsonStr.isNullOrBlank()) {
                return Pair(false, "Invalid .piuu bundle archive: Missing 'plugin.json' manifest.")
            }

            val (valid, manifest) = PiuuPluginSdk.validateManifest(manifestJsonStr!!)
            if (!valid || manifest == null) {
                return Pair(false, "Corrupted plugin.json manifest inside archive.")
            }

            val marketplaceCore = MarketplaceCore.getInstance(context)
            val installResult = marketplaceCore.registerCustomPluginFromManifest(manifestJsonStr!!, manifestJsonStr!!)

            return Pair(true, "Successfully extracted and installed .piuu bundle: '${manifest.name}' v${manifest.version}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unpack .piuu bundle archive", e)
            return Pair(false, "Extraction error: ${e.localizedMessage}")
        }
    }

    /**
     * Compute SHA-256 integrity hash of .piuu package archive.
     */
    fun computePackageHash(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
