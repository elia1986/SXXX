package com.Strip

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class StripProvider : MainAPI() {
    override var mainUrl = "https://stripchat.com"
    override var name = "Strip"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    // Usiamo ESATTAMENTE la struttura dell'URL che hai postato tu
    // Ho tenuto i parametri chiave: guestHash, isRevised, nic, e primaryTag
    override val mainPage = mainPageOf(
        "/api/front/v2/models?primaryTag=girls&limit=90&isRevised=true&nic=true&guestHash=a1ba5b85cbcd82cb9c6be570ddfa8a266f6461a38d55b89ea1a5fb06f0790f60" to "Girls",
        "/api/front/v2/models?primaryTag=couples&limit=90&isRevised=true&nic=true&guestHash=a1ba5b85cbcd82cb9c6be570ddfa8a266f6461a38d55b89ea1a5fb06f0790f60" to "Couples",
        "/api/front/v2/models?primaryTag=trans&limit=90&isRevised=true&nic=true&guestHash=a1ba5b85cbcd82cb9c6be570ddfa8a266f6461a38d55b89ea1a5fb06f0790f60" to "Trans",
    )

    private fun getHeaders() = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
        "Accept" to "application/json",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl${request.data}"
        val res = app.get(url, headers = getHeaders())
        
        // Questo trasforma il testo JSON in oggetti Kotlin
        val response = res.parsedSafe<StripResponse>()
        
        // Estraiamo la lista "models" dal JSON
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                name = model.username ?: "Unknown",
                url = "$mainUrl/${model.username}",
                type = TvType.Live,
            ).apply {
                // Costruiamo l'URL dell'immagine usando il dominio che hai trovato
                val thumb = model.previewUrl ?: model.thumbUrl ?: model.preview?.url
                this.posterUrl = when {
                    thumb == null -> null
                    thumb.startsWith("http") -> thumb
                    thumb.startsWith("//") -> "https:$thumb"
                    else -> "https://img.doppiocdn.net/${thumb.removePrefix("/")}"
                }
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = responseList.isNotEmpty()
        )
    }

    // Le "scatole" dove il plugin mette i dati ricevuti dal sito
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
