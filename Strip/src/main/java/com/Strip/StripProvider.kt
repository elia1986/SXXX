package com.Strip

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.*

class StripProvider : MainAPI() {
    override var mainUrl              = "https://stripchat.com"
    override var name                 = "Strip"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    // Aggiornato con primaryTag come visto nel tuo URL
    override val mainPage = mainPageOf(
        "/api/front/v2/models?primaryTag=girls&limit=90" to "Girls",
        "/api/front/v2/models?primaryTag=couples&limit=90" to "Couples",
        "/api/front/v2/models?primaryTag=girls&tag=italian&limit=90" to "Italians",
        "/api/front/v2/models?primaryTag=girls&tag=anal&limit=90" to "Anal",
        "/api/front/v2/models?primaryTag=girls&tag=bigBoobs&limit=90" to "BigB",
    )

    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to "$mainUrl/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page <= 1) 0 else 90 * (page - 1)
        val url = "$mainUrl${request.data}&offset=$offset&nic=true"
        
        // Usiamo gli headers per "fingere" di essere il sito stesso
        val response = app.get(url, headers = getHeaders()).parsedSafe<StripResponse>()
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                // Proviamo a prendere l'URL in ogni modo possibile
                this.posterUrl = model.previewUrl ?: model.thumbUrl ?: model.preview?.url
            }
        } ?: emptyList()

        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true), hasNext = true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/api/front/v2/models?query=$query&limit=90&nic=true"
        val response = app.get(url, headers = getHeaders()).parsedSafe<StripResponse>()
        return response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                this.posterUrl = model.previewUrl ?: model.thumbUrl ?: model.preview?.url
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val response = app.get(url, headers = getHeaders())
        val document = response.document
        val title = document.selectFirst("meta[property='og:title']")?.attr("content") ?: "Strip Live"
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content")

        return newLiveStreamLoadResponse(
            name = title,
            url = url,
            dataUrl = url,
        ).apply {
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
        
        // Regex super-flessibile per catturare i domini doppiocdn o edge-hls
        val hlsRegex = """hlsStreamUrl"[:\s]+"(https?[^"]+\.m3u8[^"]*)""".toRegex()
        val rawUrl = hlsRegex.find(html)?.groups?.get(1)?.value
        
        val hlsUrl = rawUrl?.replace("\\/", "/")?.replace("\\u002F", "/")

        if (!hlsUrl.isNullOrBlank()) {
            callback.invoke(
                newExtractorLink(name, name, hlsUrl, "$mainUrl/", ExtractorLinkType.M3U8)
            )
            return true
        }
        return false
    }

    // Classi JSON potenziate
    data class StripPreview(
        @JsonProperty("url") val url: String? = null
    )

    data class StripModel(
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("previewUrl") val previewUrl: String? = null,
        @JsonProperty("thumbUrl") val thumbUrl: String? = null,
        @JsonProperty("preview") val preview: StripPreview? = null, // Per oggetti annidati
    )

    data class StripResponse(
        @JsonProperty("models") val models: List<StripModel> = emptyList()
    )
}
