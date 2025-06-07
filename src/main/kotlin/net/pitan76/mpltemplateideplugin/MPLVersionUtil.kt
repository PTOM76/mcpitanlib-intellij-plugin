package net.pitan76.mpltemplateideplugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class MPLVersionUtil() {
    private val client = OkHttpClient()
    private val mavenUrl = "https://maven.pitan76.net/net/pitan76/mcpitanlib-fabric+%s/maven-metadata.xml"

    suspend fun getVersions(mcversion: String): List<String> {
        return withContext(Dispatchers.IO) { // 非同期のために Dispatchers.IO を使うらしい
            try {
                val url = mavenUrl.format(mcversion)
                val request = Request.Builder().url(url).build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()

                val xml = response.body?.string() ?: return@withContext emptyList()
                parseVersions(xml)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getLatestVersion(mcversion: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val versions = getVersions(mcversion)
                versions.lastOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
                 null
            }
        }
    }

    private fun parseVersions(xml: String): List<String> {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document: Document = builder.parse(ByteArrayInputStream(xml.toByteArray()))

            val versions = document.getElementsByTagName("version")
            val versionList = mutableListOf<String>()
            for (i in 0 until versions.length)
                versionList.add(versions.item(i).textContent)

            versionList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}