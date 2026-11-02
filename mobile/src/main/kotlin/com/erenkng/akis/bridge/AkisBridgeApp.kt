package com.erenkng.akis.bridge

import android.app.Application

class AkisBridgeApp : Application() {

    lateinit var graph: BridgeGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = BridgeGraph(this)
        graph.ensureTracking()
    }
}

val android.content.Context.bridgeGraph: BridgeGraph
    get() = (applicationContext as AkisBridgeApp).graph
