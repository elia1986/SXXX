package com.Strip

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class StripPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(StripProvider())
    }
}
