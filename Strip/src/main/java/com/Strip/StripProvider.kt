package com.Strip

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class StripProvider : MainAPI() {
    override var mainUrl              = "https://stripchat.com"
    override var name                 = "Strip"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "/api/front/v2/models?primaryTag=girls&limit=90" to "Girls",
        "/api/front/v2/models?primaryTag=couples&limit=90" to "Couples",
        "/api/front/v2/models?primaryTag=girls&tag=italian&limit=90" to "Italians",
    )

    private fun getHeaders() = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    )

    // Funzione potenziata per le immagini basata sul tuo esempio doppiocdn
    private fun fixUrl(url: String?): String? {
        if (url == null) return null
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            // Se l'URL sembra un percorso relativo, lo puntiamo al CDN che mi hai indicato
            url.startsWith("/thumbs") -> "https://img.doppiocdn.net$url"
            else -> url
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page <= 1) 0 else 90 * (page - 1)
        val url = "$mainUrl${request.data}&offset=$offset&nic=true"
        
        val response = app.get(url, headers = getHeaders()).parsedSafe<StripResponse>()
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                // Proviamo a pescare l'immagine da tutti i campi possibili
                val rawImg = model.previewUrl ?: model.preview?.url ?: model.thumbUrl
                this.posterUrl = fixUrl(rawImg)
            }
        } ?: emptyList()

        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true), hasNext = true)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = getHeaders()).document
        val title = document.selectFirst("meta[property='og:title']")?.attr("content") ?: "Strip Live"
        val poster = fixUrl(document.selectFirst("meta[property='og:image']")?.attr("content"))

        return newLiveStreamLoadResponse(title, url, url).apply {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = app.get(data, headers = getHeaders()).text
        
        val hlsRegex = """hlsStreamUrl"[:\s]+"(https?[^"]+\.m3u8[^"]*)""".toRegex()
        val match = hlsRegex.find(html)?.groups?.get(1)?.value
        val hlsUrl = match?.replace("\\/", "/")?.replace("\\u002F", "/")

        if (!hlsUrl.isNullOrBlank()) {
            // USIAMO IL COSTRUTTORE DIRETTO PER EVITARE ERRORI DI COMPILAZIONE
            callback.invoke(
                ExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = hlsUrl,
                    referer = "$mainUrl/",
                    quality = Qualities.P1080.value, // Forziamo una qualità
                    type = ExtractorLinkType.M3U8
                )
            )
            return true
        }
        return false
    }

    data class StripPreview(
        @JsonProperty("url") val url: String? = null
    )

    data class StripModel(
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("previewUrl") val previewUrl: String? = null,
        @JsonProperty("thumbUrl") val thumbUrl: String? = null,
        @JsonProperty("preview") val preview: StripPreview? = null,
    )

    data class StripResponse(
        @JsonProperty("models") val models: List<StripModel> = emptyList()
    )
}
