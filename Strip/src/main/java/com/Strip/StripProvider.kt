package com.Strip

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class StripProvider : MainAPI() {
    override var mainUrl              = "https://stripchat.com"
    override var name                 = "Strip"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val supportedTypes       = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "/api/front/v2/models?primaryTag=girls" to "Girls",
        "/api/front/v2/models?primaryTag=couples" to "Couples",
        "/api/front/v2/models?primaryTag=men" to "Men",
        "/api/front/v2/models?primaryTag=trans" to "Trans",
    )

    private fun getHeaders(): Map<String, String> {
        return mapOf(
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to "$mainUrl/",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Accept" to "application/json, text/plain, */*"
        )
    }

    private fun fixUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            else -> "https://img.doppiocdn.net${if (url.startsWith("/")) "" else "/"}$url"
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val limit = 90
        val offset = (page - 1) * limit
        // Aggiungiamo i parametri che abbiamo visto nel tuo URL originale
        val url = "$mainUrl${request.data}&limit=$limit&offset=$offset&nic=true&isRevised=true"
        
        val res = app.get(url, headers = getHeaders())
        
        // Se la risposta è vuota, logghiamo per debug interno
        if (res.text.isBlank()) return newHomePageResponse(emptyList<HomePageList>(), false)

        val response = res.parsedSafe<StripResponse>()
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "Unknown",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                // Proviamo a ricostruire l'immagine dal thumbUrl o previewUrl
                val rawImg = model.previewUrl ?: model.thumbUrl ?: model.preview?.url
                this.posterUrl = fixUrl(rawImg)
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = responseList.isNotEmpty()
        )
    }

    data class StripPreview(@JsonProperty("url") val url: String? = null)

    data class StripModel(
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("previewUrl") val previewUrl: String? = null,
        @JsonProperty("thumbUrl") val thumbUrl: String? = null,
        @JsonProperty("preview") val preview: StripPreview? = null,
    )

    data class StripResponse(
        @JsonProperty("models") val models: List<StripModel>? = null
    )
}
