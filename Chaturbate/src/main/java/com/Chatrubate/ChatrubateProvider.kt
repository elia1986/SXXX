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

    override val mainPage = mainPageOf(
        "/api/ts/roomlist/room-list/?limit=90" to "Featured",
        "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
        "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page <= 1) 0 else 90 * (page - 1)
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
        return newLiveStreamLoadResponse(
            name = url.substringAfterLast("/").replace("/", ""),
            url = url,
            dataUrl = url,
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).text
        val hlsRegex = """hls_source":\s*"(https:.*?\.m3u8)"""".toRegex()
        val m3u8Url = hlsRegex.find(doc)?.groupValues?.get(1)?.replace("\\u002D", "-")?.replace("\\/", "/")

        if (m3u8Url != null) {
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    m3u8Url,
                    "$mainUrl/",
                    Qualities.Unknown.value,
                    true
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
