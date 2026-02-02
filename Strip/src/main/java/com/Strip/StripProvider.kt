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

    // Configurazione categorie basata sul parametro primaryTag
    override val mainPage = mainPageOf(
        "/api/front/v2/models?primaryTag=girls&limit=90" to "Girls",
        "/api/front/v2/models?primaryTag=couples&limit=90" to "Couples",
        "/api/front/v2/models?primaryTag=men&limit=90" to "Men",
        "/api/front/v2/models?primaryTag=trans&limit=90" to "Trans",
        "/api/front/v2/models?primaryTag=girls&tag=italian&limit=90" to "Italians",
    )

    private fun getHeaders() = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    )

    // Logica specifica per le immagini doppiocdn
    private fun fixUrl(url: String?): String? {
        if (url == null) return null
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            // Se l'API restituisce solo /thumbs/..., aggiungiamo il dominio corretto
            url.startsWith("/thumbs") -> "https://img.doppiocdn.net$url"
            else -> url
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Calcolo offset per la paginazione
        val offset = if (page <= 1) 0 else 90 * (page - 1)
        val url = "$mainUrl${request.data}&offset=$offset&nic=true"
        
        val response = app.get(url, headers = getHeaders()).parsedSafe<StripResponse>()
        
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "Unknown",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                // Proviamo a estrarre l'immagine dai vari campi JSON
                val rawImg = model.previewUrl ?: model.preview?.url ?: model.thumbUrl
                this.posterUrl = fixUrl(rawImg)
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = true
        )
    }

    // Classi per mappare il JSON dell'API
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
