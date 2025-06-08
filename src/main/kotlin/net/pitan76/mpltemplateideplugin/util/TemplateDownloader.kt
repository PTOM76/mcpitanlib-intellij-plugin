package net.pitan76.mpltemplateideplugin.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class TemplateDownloader {
    private val client = OkHttpClient()
    
    fun downloadTemplate(targetPath: String, repoName: String) {
        val url = "https://github.com/$repoName/archive/refs/heads/main.zip"
        val request = Request.Builder().url(url).build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw RuntimeException(Lang.get("message.faileddownload"))
        }
        
        val zipInputStream = ZipInputStream(response.body!!.byteStream())
        var entry = zipInputStream.nextEntry
        
        while (entry != null) {
            if (!entry.isDirectory) {
                val relativePath = entry.name.substringAfter('/')
                if (relativePath.isNotEmpty()) {
                    val file = File(targetPath, relativePath)
                    file.parentFile.mkdirs()
                    
                    FileOutputStream(file).use { output ->
                        zipInputStream.copyTo(output)
                    }
                }
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        
        zipInputStream.close()
    }
}