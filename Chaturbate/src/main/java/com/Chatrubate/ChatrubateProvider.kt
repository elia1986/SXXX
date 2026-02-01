package com.Chatrubate

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class ChatrubateProvider : MainAPI() {
    override var mainUrl              = "https://chaturbate.com"
    override var name                 = "Chatrubate"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "/api/ts/roomlist/room-list/?limit=90" to "Featured",
        "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
        "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page <= 1) 0 else 90 * (page - 1)
        // parsedSafe gestisce già la null-safety evitando crash se l'API non risponde
        val response = app.get("$mainUrl${request.data}&offset=$offset").parsedSafe<Response>()
        
        val responseList = response?.rooms?.map { room ->
            newLiveSearchResponse(
                name = room.username,
                url = "$mainUrl/${room.username}",
                type = TvType.Live,
            ).apply {
                this.posterUrl = room.img
            }
        } ?: emptyList()
        
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true), hasNext = true)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // Chaturbate supporta i tag nella ricerca API
        val response = app.get("$mainUrl/api/ts/roomlist/room-list/?hashtags=$query&limit=90").parsedSafe<Response>()
        return response?.rooms?.map { room ->
            newLiveSearchResponse(
                name = room.username,
                url = "$mainUrl/${room.username}",
                type = TvType.Live,
            ).apply {
                this.posterUrl = room.img
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        // Semplifichiamo per velocizzare l'apertura: evitiamo document.select se non serve
        val name = url.substringAfterLast("/").replace("/", "")
        return newLiveStreamLoadResponse(
            name = name,
            url = url,
            dataUrl = url,
        ).apply {
            this.posterUrl = "https://roomimage.com/dbimages/previews/${name.lowercase()}.jpg"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Carichiamo solo il testo per cercare il link m3u8 velocemente
        val responseText = app.get(data).text
        
        // Regex migliorata per catturare l'HLS source pulito
        val hlsRegex = """hls_source":\s*"(https:.*?\.m3u8)"""".toRegex()
        val match = hlsRegex.find(responseText)
        
        val m3u8Url = match?.groupValues?.get(1)
            ?.replace("\\u002D", "-")
            ?.replace("\\/", "/")

        if (m3u8Url != null) {
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = m3u8Url,
                    referer = "$mainUrl/",
                    quality = Qualities.Unknown.value, // Lascia che il player decida la qualità migliore
                    isM3u8 = true
                )
            )
        }
        return true
    }

    data class Room(
        @JsonProperty("img") val img: String = "",
        @JsonProperty("username") val username: String = "",
    )

    data class Response(
        @JsonProperty("rooms") val rooms: List<Room> = arrayListOf()
    )
}
