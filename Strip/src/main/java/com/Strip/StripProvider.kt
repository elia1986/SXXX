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

    // Configurazione Homepage Stripchat
    override val mainPage = mainPageOf(
        "/api/front/v2/models?primaryGenre=female&limit=90" to "Female",
        "/api/front/v2/models?primaryGenre=couple&limit=90" to "Couples",
        "/api/front/v2/models?primaryGenre=female&tag=anal&limit=90" to "Female Anal",
        "/api/front/v2/models?primaryGenre=female&tag=italian&limit=90" to "Italians",
        "/api/front/v2/models?primaryGenre=female&tag=bigBoobs&limit=90" to "BigB",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page <= 1) 0 else 90 * (page - 1)
        val url = "$mainUrl${request.data}&offset=$offset"
        
        val response = app.get(url).parsedSafe<StripResponse>()
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                this.posterUrl = model.previewUrl ?: model.thumbUrl
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/api/front/v2/models?query=$query&limit=90").parsedSafe<StripResponse>()
        return response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                this.posterUrl = model.previewUrl
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1")?.text() ?: "Strip Live"
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
        val doc = app.get(data).document
        // Stripchat espone i dati della room in un JSON dentro uno script
        val script = doc.select("script").find { it.html().contains("window.__PRELOADED_STATE__") }
        val html = script?.html() ?: return false
        
        // Estrazione URL HLS dalla configurazione precaricata
        val hlsUrl = "\"hlsStreamUrl\":\"(.*?)\"".toRegex().find(html)?.groups?.get(1)?.value
            ?.replace("\\u002F", "/")

        if (hlsUrl != null) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = hlsUrl,
                    referer = "$mainUrl/",
                    type = ExtractorLinkType.M3U8
                )
            )
        }
        return true
    }

    // Classi per il parsing JSON di Stripchat
    data class StripModel(
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("previewUrl") val previewUrl: String? = null,
        @JsonProperty("thumbUrl") val thumbUrl: String? = null,
    )

    data class StripResponse(
        @JsonProperty("models") val models: List<StripModel> = emptyList()
    )
}
