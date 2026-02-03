package com.Strip

import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.nodes.Element
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
        "men" to "Men",
        "trans" to "Trans",
    )

    private fun getHeaders() = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
        "Accept" to "application/json"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = (page - 1) * 60
        val url = "$mainUrl/api/front/v2/models?primaryTag=${request.data}&offset=$offset&$apiParams"
        
        val response = app.get(url, headers = getHeaders()).parsedSafe<Response>()
        
        val responseList = response?.models?.map { model ->
            newLiveSearchResponse(
                model.username ?: "Unknown",
                "$mainUrl/${model.username}",
                TvType.Live,
            ).apply {
                this.posterUrl = model.previewUrl ?: model.thumbUrl ?: "https://img.doppiocdn.net/${model.preview?.url?.trimStart('/')}"
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = responseList.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title = this.select(".model-list-item-username").text()
        val href = this.select(".model-list-item-link").attr("href").let { 
            if (it.startsWith("http")) it else mainUrl + it 
        }
        val posterUrl = this.selectFirst(".image-background")?.attr("src")
        
        return newLiveSearchResponse(title, href, TvType.Live).apply {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/search/models/$query", headers = getHeaders()).document
        return doc.select(".model-list-item").map { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, headers = getHeaders()).document
        val title = document.selectFirst("meta[property='og:title']")?.attr("content")?.replace(" | XHamsterLive", "")
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content")
        val description = document.selectFirst("meta[property='og:description']")?.attr("content")

        return newLiveStreamLoadResponse(
            title ?: "Live Show",
            url,
            TvType.Live,
            url,
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
        val doc = app.get(data).document
        val script = doc.select("script").find { it.html().contains("window.__PRELOADED_STATE__") }
            ?: return false
            
        val json = script.html().unescapeUnicode()
        
        val streamName = json.substringAfter("\"streamName\":\"").substringBefore("\",")
        val streamHost = json.substringAfter("\"hlsStreamHost\":\"").substringBefore("\",")
        val hlsUrlTemplate = json.substringAfter("\"hlsStreamUrlTemplate\":\"").substringBefore("\",")
        
        if (streamName.isNotBlank() && streamHost.isNotBlank()) {
            val finalm3u8Url = hlsUrlTemplate
                .replace("{cdnHost}", streamHost)
                .replace("{streamName}", streamName)
                .replace("{suffix}", "_auto")

            // Usiamo il metodo posizionale per evitare errori di nomi parametri
            callback.invoke(
                newExtractorLink(
                    name,
                    name,
                    finalm3u8Url,
                    data,
                    Qualities.Unknown.value,
                    true
                )
            )
            return true
        }
        return false
    }

    data class Preview(@JsonProperty("url") val url: String? = null)

    data class Model(
        @JsonProperty("id") val id: String? = null,
        @JsonProperty("username") val username: String? = null,
        @JsonProperty("previewUrl") val previewUrl: String? = null,
        @JsonProperty("thumbUrl") val thumbUrl: String? = null,
        @JsonProperty("preview") val preview: Preview? = null
    )

    data class Response(
        @JsonProperty("models") val models: List<Model> = arrayListOf()
    )
}

fun String.unescapeUnicode() = replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) {
    String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
}
