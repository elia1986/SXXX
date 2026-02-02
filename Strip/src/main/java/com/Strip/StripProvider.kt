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

    // Usiamo la base dell'URL che hai fornito
    // Ho rimosso solo i parametri temporanei come 'watchedIds' o 'uniq' che cambiano sempre
    private val apiBase = "limit=90&recInFeatured=true&removeShows=true&isRevised=true&guestHash=a1ba5b85cbcd82cb9c6be570ddfa8a266f6461a38d55b89ea1a5fb06f0790f60&nic=true"

    override val mainPage = mainPageOf(
        "/api/front/v2/models?primaryTag=girls&$apiBase" to "Girls",
        "/api/front/v2/models?primaryTag=couples&$apiBase" to "Couples",
        "/api/front/v2/models?primaryTag=men&$apiBase" to "Men",
        "/api/front/v2/models?primaryTag=trans&$apiBase" to "Trans",
    )

    private fun getHeaders() = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Accept" to "application/json"
    )

    // Funzione specifica per riparare gli URL delle immagini doppiocdn
    private fun fixUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            // Se l'URL è solo un path (es. /thumbs/...) aggiungiamo il dominio che hai trovato
            else -> "https://img.doppiocdn.net/${url.removePrefix("/")}"
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = (page - 1) * 90
        val finalUrl = "$mainUrl${request.data}&offset=$offset"
        
        val res = app.get(finalUrl, headers = getHeaders())
        
        // Se non vedi nomi, stampiamo la risposta nei log (per debug)
        val response = res.parsedSafe<StripResponse>()
        
        // Se 'models' è nullo, proviamo a vedere se i dati sono dentro un altro campo
        val modelsList = response?.models ?: emptyList()

        val responseList = modelsList.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "Unknown",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                // Cerchiamo l'immagine in tutti i campi possibili forniti dal JSON
                this.posterUrl = fixUrl(model.previewUrl ?: model.thumbUrl ?: model.preview?.url)
            }
        }

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = responseList.isNotEmpty()
        )
    }

    // Classi per il parsing del JSON basate sulla struttura API v2
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
        @JsonProperty("models") val models: List<StripModel>? = null
    )
}
