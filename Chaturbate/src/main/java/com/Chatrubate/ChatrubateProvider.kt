package com.Chatrubate

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.*

class ChatrubateProvider : MainAPI() {
    override var mainUrl              = "https://chaturbate.com"
    override var name                 = "Chatrubate"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    // 1. Partiamo subito con Female e Couples (Immagine Featured Rimossa)
    override val mainPage = mainPageOf(
        "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
        "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val offset = if (page == 1) 0 else 90 * (page - 1)
        val response = app.get("$mainUrl${request.data}&offset=$offset").parsedSafe<Response>()
        val responseList = response?.rooms?.map { room ->
            newLiveSearchResponse(
                room.username,
                "$mainUrl/${room.username}",
                TvType.Live,
            ).apply {
                this.posterUrl = room.img
            }
        } ?: emptyList()

        return newHomePageResponse(
            HomePageList(request.name, responseList, isHorizontalImages = true),
            hasNext = true
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<LiveSearchResponse>()
        for (i in 0..3) {
            val response = app.get("$mainUrl/api/ts/roomlist/room-list/?hashtags=$query&limit=90&offset=${i * 90}").parsedSafe<Response>()
            val results = response?.rooms?.map { room ->
                newLiveSearchResponse(
                    room.username,
                    "$mainUrl/${room.username}",
                    TvType.Live,
                ).apply {
                    this.posterUrl = room.img
                }
            } ?: emptyList()

            if (results.isNotEmpty() && !searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")
            ?.trim()?.replace("| PornHoarder.tv", "") ?: "Chaturbate Live"
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        return newLiveStreamLoadResponse(
            title,
            url,
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
        val script = doc.select("script").find { it.html().contains("window.initialRoomDossier") }
        val json = script?.html()?.substringAfter("window.initialRoomDossier = \"")?.substringBefore(";")?.unescapeUnicode()
        val m3u8Url = "\"hls_source\": \"(.*).m3u8\"".toRegex().find(json ?: "")?.groups?.get(1)?.value

        if (m3u8Url != null) {
            try {
                // 2. Correzione definitiva per Gradle: 4 parametri + blocco config
                callback.invoke(
                    newExtractorLink(
                        this.name,
                        this.name,
                        "$m3u8Url.m3u8",
                        ExtractorLinkType.M3U8
                    ) {
                        this.referer = "$mainUrl/"
                    }
                )
            } catch (e: Exception) {
                logError(e)
            }
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

fun String.unescapeUnicode() = replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) {
    String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
}
