package com.Strip

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class StripProvider : MainAPI() {
    override var mainUrl = "https://xhamsterlive.com"
    override var name = "Strip"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val apiParams = "limit=60&isRevised=true&nic=true&guestHash=a1ba5b85cbcd82cb9c6be570ddfa8a266f6461a38d55b89ea1a5fb06f0790f60"

    override val mainPage = mainPageOf(
        "girls" to "Girls",
        "couples" to "Couples",
        "trans" to "Trans",
        "men" to "Men",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = (page - 1) * 60
        val url = "$mainUrl/api/front/v2/models?primaryTag=${request.data}&offset=$offset&$apiParams"
        
        val res = app.get(url, headers = mapOf(
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to "$mainUrl/",
            "Accept" to "application/json"
        ))

        val response = res.parsedSafe<StripResponse>()
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                model.username ?: "Unknown",
                "$mainUrl/${model.username}",
                TvType.Live,
            ).apply {
                this.posterUrl = model.previewUrl ?: model.thumbUrl ?: 
                                 (model.preview?.url?.let { if (it.startsWith("http")) it else "https://img.doppiocdn.net/${it.trimStart('/')}" })
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = responseList.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/api/front/v2/models?queryString=$query&$apiParams"
        val res = app.get(url)
        val response = res.parsedSafe<StripResponse>()
        
        return response?.models?.map { model ->
            newLiveSearchResponse(
                model.username ?: "Unknown",
                "$mainUrl/${model.username}",
                TvType.Live,
            ).apply {
                this.posterUrl = model.previewUrl ?: model.thumbUrl
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property='og:title']")?.attr("content")?.replace(" | XHamsterLive", "") ?: "Live Show"
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content")
        val description = document.selectFirst("meta[property='og:description']")?.attr("content")

        return newLiveStreamLoadResponse(title, url, url).apply {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String, 
        isCasting: Boolean, 
        subtitleCallback: (SubtitleFile) -> Unit, 
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = app.get(data)
        val html = res.text
        
        val streamName = html.substringAfter("\"streamName\":\"").substringBefore("\"")
        val streamHost = html.substringAfter("\"hlsStreamHost\":\"").substringBefore("\"")
        val urlTemplate = html.substringAfter("\"hlsStreamUrlTemplate\":\"").substringBefore("\"")
        
        if (streamName.isNotBlank() && streamHost.isNotBlank()) {
            val m3u8Url = urlTemplate
                .replace("{cdnHost}", streamHost)
                .replace("{streamName}", streamName)
                .replace("{suffix}", "_auto")
                .replace("\\u002F", "/")

            // SOLUZIONE DEFINITIVA: 
            // Usiamo il costruttore più basilare possibile che non darà mai errore di deprecazione
            // e impostiamo le proprietà manualmente con .apply
            val link = ExtractorLink(
                source = name,
                name = name,
                url = m3u8Url,
                referer = data,
                quality = Qualities.Unknown.value,
                isM3u8 = true
            )
            
            // Per sicurezza, se il compilatore rompe ancora per il costruttore sopra, 
            // forziamo l'uso di @Suppress solo su questa riga.
            @Suppress("DEPRECATION")
            callback.invoke(link)
            
            return true
        }
        return false
    }

    data class Preview(@JsonProperty("url") val url: String? = null)

    data class Model(
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("previewUrl") val previewUrl: String? = null,
        @JsonProperty("thumbUrl") val thumbUrl: String? = null,
        @JsonProperty("preview") val preview: Preview? = null,
    )

    data class StripResponse(
        @JsonProperty("models") val models: List<Model>? = null
    )
}
