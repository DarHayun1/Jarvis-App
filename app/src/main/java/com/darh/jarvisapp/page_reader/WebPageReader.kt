package com.darh.jarvisapp.page_reader

import com.darh.jarvisapp.api.OPEN_AI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebPageReader @Inject constructor() {

    private val client = OkHttpClient()

    suspend fun readFromUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            val body = fetchWebPage(url)
            body?.let { parseWebPage(it) }
        }
    }

    private suspend fun fetchWebPage(url: String): String? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.tag(OPEN_AI).e(e, "fetched web page failed.")
                null
            }
        }
    }

    private suspend fun parseWebPage(html: String): String {
        return withContext(Dispatchers.IO) {
            val document = Jsoup.parse(html)
            document.text()
        }
    }
}
