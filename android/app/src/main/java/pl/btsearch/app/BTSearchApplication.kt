package pl.btsearch.app

import android.app.Application
import org.maplibre.android.MapLibre

class BTSearchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}
