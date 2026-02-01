package com.Chatrubate

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.Qualities

class ChatrubateProvider : MainAPI() {

    override var mainUrl = "https://chaturbate.com"
    override var name = "Chatrubate"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "/api/ts/roomlist/room-list/?limit=90" to "Featured",
        "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
        "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val offset = if (page <= 1) 0 else 90 * (page - 1)

        val response = app.get(
            "$mainUrl${request.data}&offset=$offset"
        ).parsedSafe<Response>()

        val rooms = response?.rooms?.map { room ->
            newLiveSearchResponse(
                room.username,
                "$mainUrl/${room.username}",
                TvType.Live
            ).apply {
                posterUrl = room.img
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, rooms, true),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get(
            "$mainUrl/api/ts/roomlist/room-list/?hashtags=$query&limit=90"
        ).parsedSafe<Response>()

        return response?.rooms?.map { room ->
            newLiveSearchResponse(
                room.username,
                "$mainUrl/${room.username}",
                TvType.Live
            ).apply {
                posterUrl = room.img
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        return newLiveStreamLoadResponse(
            url.substringAfterLast("/"),
            url,
            url
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val html = app.get(data).text

        // Regex robusta per stream HLS
        val regex = Regex("""https://[^"]+\.m3u8""")
        val m3u8Url = regex.find(html)?.value ?: return false

        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                m3u8Url,
                mainUrl,
                Qualities.Unknown.value,
                true
            )
        )

        return true
    }

    data class Room(
        @JsonProperty("img") val img: String = "",
        @JsonProperty("username") val username: String = ""
    )

    data class Response(
        @JsonProperty("rooms") val rooms: List<Room> = arrayListOf()
    )
}
