package com.erenkng.akis.wear

import android.app.Application
import android.content.Context

class AkisWearApp : Application() {

    lateinit var graph: WearGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = WearGraph(this)
    }
}

val Context.wearGraph: WearGraph
    get() = (applicationContext as AkisWearApp).graph
