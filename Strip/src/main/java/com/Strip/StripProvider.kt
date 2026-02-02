package com.Strip

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class StripProvider : MainAPI() {
    // Cambiamo l'URL base a XHamsterLive come richiesto
    override var mainUrl = "https://xhamsterlive.com"
    override var name = "Strip"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    // Parametri API specifici per XHamsterLive (v2)
    private val apiParams = "limit=90&isRevised=true&nic=true&guestHash=a1ba5b85cbcd82cb9c6be570ddfa8a266f6461a38d55b89ea1a5fb06f0790f60"

    override val mainPage = mainPageOf(
        "girls" to "Girls",
        "couples" to "Couples",
        "trans" to "Trans",
        "men" to "Men",
    )

    private fun getHeaders() = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
        "Accept" to "application/json",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // STEP 1: Inizializzazione sessione per gestire i cookie di XHamsterLive
        app.get(mainUrl, headers = getHeaders())

        // STEP 2: Chiamata all'endpoint v2
        val offset = (page - 1) * 90
        val url = "$mainUrl/api/front/v2/models?primaryTag=${request.data}&$apiParams&offset=$offset"
        
        val res = app.get(url, headers = getHeaders())
        
        // Parsing della risposta JSON
        val response = res.parsedSafe<StripResponse>()
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "Unknown",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                // Utilizziamo il server immagini doppiocdn.net citato nel codice di rikacelery
                val thumb = model.previewUrl ?: model.thumbUrl ?: model.preview?.url
                this.posterUrl = when {
                    thumb == null -> null
                    thumb.startsWith("http") -> thumb
                    thumb.startsWith("//") -> "https:$thumb"
                    else -> "https://img.doppiocdn.net/${thumb.trimStart('/')}"
                }
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = responseList.isNotEmpty()
        )
    }

    // Strutture dati per il mapping del JSON di XHamsterLive
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
