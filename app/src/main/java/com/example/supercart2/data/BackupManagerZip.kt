package com.example.supercart2.data

import android.content.Context
import android.util.Log
import com.example.supercart2.data.BackupManager.BackupData
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ZIP layout of a BackupManager backup: data.json + images/<uuid>.jpg

private const val ZIP_DATA_ENTRY = "data.json"
private const val ZIP_IMAGES_DIR = "images/"

/** What was found in a backup ZIP: the data (null when data.json is missing) and images by uuid. */
internal class BackupZipContents(
    var data: BackupData? = null,
    val images: MutableMap<String, ByteArray> = mutableMapOf(),
)

/** Writes data.json, then the images of non-deleted groceries that exist locally. */
internal fun writeBackupZip(backupFile: File, data: BackupData, context: Context) {
    val jsonContent = BackupManager.gson.toJson(data)
    ZipOutputStream(FileOutputStream(backupFile)).use { zip ->
        zip.writeEntry(ZIP_DATA_ENTRY) { write(jsonContent.toByteArray(Charsets.UTF_8)) }
        data.groceries.filter { !it.deleted && it.imageUUID != null }
            .forEach { zip.writeImage(it.imageUUID!!, context) }
    }
}

private fun ZipOutputStream.writeImage(uuid: String, context: Context) {
    val imageFile = ImageManager.getLocalImageFile(uuid, context)
    if (imageFile == null || !imageFile.exists()) return
    writeEntry("$ZIP_IMAGES_DIR$uuid.jpg") { imageFile.inputStream().use { it.copyTo(this) } }
    Log.d("BackupManager", "  ↳ packed image $uuid")
}

private inline fun ZipOutputStream.writeEntry(name: String, body: ZipOutputStream.() -> Unit) {
    putNextEntry(ZipEntry(name))
    body()
    closeEntry()
}

/** Reads data.json (the last one wins) and every file under images/. */
internal fun readBackupZip(zip: ZipInputStream): BackupZipContents {
    val contents = BackupZipContents()
    generateSequence { zip.nextEntry }.forEach { entry ->
        contents.read(zip, entry)
        zip.closeEntry()
    }
    return contents
}

private fun BackupZipContents.read(zip: ZipInputStream, entry: ZipEntry) {
    when {
        entry.name == ZIP_DATA_ENTRY ->
            data = BackupManager.gson.fromJson(zip.readBytes().toString(Charsets.UTF_8), BackupData::class.java)
        entry.name.startsWith(ZIP_IMAGES_DIR) && !entry.isDirectory ->
            images[File(entry.name).nameWithoutExtension] = zip.readBytes()
    }
}
