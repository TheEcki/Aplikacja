package pl.btsearch.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import pl.btsearch.app.data.BtsApi

enum class Tab(val label: String, val icon: ImageVector) {
    MAP("Mapa", Icons.Filled.Place),
    SEARCH("Szukaj", Icons.Filled.Search),
    ABOUT("O aplikacji", Icons.Filled.Info),
}

@Composable
fun AppRoot(api: BtsApi) {
    var tab by rememberSaveable { mutableStateOf(Tab.MAP) }
    var stationId by rememberSaveable { mutableStateOf<Int?>(null) }

    // Wstecz zamyka szczegóły stacji zamiast wychodzić z aplikacji.
    BackHandler(enabled = stationId != null) { stationId = null }

    Scaffold(
        bottomBar = {
            if (stationId == null) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val openId = stationId
            if (openId != null) {
                StationDetailScreen(api = api, stationId = openId, onBack = { stationId = null })
            } else {
                when (tab) {
                    Tab.MAP -> MapScreen(api = api, onStationClick = { stationId = it })
                    Tab.SEARCH -> SearchScreen(api = api, onStationClick = { stationId = it })
                    Tab.ABOUT -> AboutScreen()
                }
            }
        }
    }
}
