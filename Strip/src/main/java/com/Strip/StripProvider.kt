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
                // Tentiamo vari campi per l'immagine
                this.posterUrl = model.previewUrl ?: model.thumbUrl ?: model.snapshotUrl ?: model.avatarUrl
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
                this.posterUrl = model.previewUrl ?: model.thumbUrl ?: model.snapshotUrl
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val response = app.get(url)
        val document = response.document
        
        // Estrazione dati dai meta tag (presenti nel sorgente che hai inviato)
        val title = document.selectFirst("meta[property='og:title']")?.attr("content") ?: "Strip Live"
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content")
        val description = document.selectFirst("meta[name='description']")?.attr("content")

        return newLiveStreamLoadResponse(
            name = title,
            url = url,
            dataUrl = url,
        ).apply {
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
        val response = app.get(data)
        val html = response.text
        
        // Regex migliorata per catturare l'URL HLS anche con caratteri di escape
        val hlsRegex = """hlsStreamUrl"[:\s]+"(https?[:\\/]+[^"]+\.m3u8[^"]*)""".toRegex()
        val match = hlsRegex.find(html)?.groups?.get(1)?.value
        
        val hlsUrl = match?.replace("\\/", "/")?.replace("\\u002F", "/")

        if (!hlsUrl.isNullOrBlank()) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = hlsUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                    // Aggiungiamo un User-Agent comune per evitare blocchi
                    this.headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }
            )
            return true
        }
        return false
    }

    // Classi JSON aggiornate con più campi per le immagini
    data class StripModel(
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("previewUrl") val previewUrl: String? = null,
        @JsonProperty("thumbUrl") val thumbUrl: String? = null,
        @JsonProperty("snapshotUrl") val snapshotUrl: String? = null,
        @JsonProperty("avatarUrl") val avatarUrl: String? = null,
    )

    data class StripResponse(
        @JsonProperty("models") val models: List<StripModel> = emptyList()
    )
}
