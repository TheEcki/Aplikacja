package pl.btsearch.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import pl.btsearch.app.data.BtsApi
import pl.btsearch.app.data.Location

private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/bright"
private const val SOURCE_ID = "stations-source"
private const val LAYER_ID = "stations-layer"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(api: BtsApi, onStationClick: (Int) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var source by remember { mutableStateOf<GeoJsonSource?>(null) }
    var locations by remember { mutableStateOf<Map<Int, Location>>(emptyMap()) }
    var selected by remember { mutableStateOf<Location?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fetchJob by remember { mutableStateOf<Job?>(null) }

    fun loadVisible(map: MapLibreMap) {
        val b = map.projection.visibleRegion.latLngBounds
        fetchJob?.cancel()
        fetchJob = scope.launch {
            delay(300) // debounce podczas przesuwania mapy
            loading = true
            error = null
            try {
                val response = api.locations(
                    south = b.latitudeSouth,
                    west = b.longitudeWest,
                    north = b.latitudeNorth,
                    east = b.longitudeEast,
                )
                val valid = response.data.filter { it.latitude != 0.0 || it.longitude != 0.0 }
                locations = valid.associateBy { it.id }
                source?.setGeoJson(
                    FeatureCollection.fromFeatures(
                        valid.map { loc ->
                            Feature.fromGeometry(Point.fromLngLat(loc.longitude, loc.latitude))
                                .also { it.addNumberProperty("id", loc.id) }
                        },
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Nie udało się pobrać danych"
            } finally {
                loading = false
            }
        }
    }

    // Cykl życia MapView musi być przekazywany z Activity (MapLibre wymaga wywołań onStart/onResume/...).
    DisposableEffect(lifecycleOwner) {
        var started = false
        var resumed = false
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> if (!resumed) { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> if (resumed) { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> if (started) { mapView.onStop(); started = false }
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) { mapView.onDestroy(); destroyed = true }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            if (!destroyed) mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(52.0, 19.4)).zoom(5.8).build(),
                ),
            )
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                val src = GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyList<Feature>()))
                style.addSource(src)
                style.addLayer(
                    CircleLayer(LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.circleRadius(7f),
                        PropertyFactory.circleColor("#E53935"),
                        PropertyFactory.circleStrokeColor("#FFFFFF"),
                        PropertyFactory.circleStrokeWidth(1.5f),
                    ),
                )
                source = src
                loadVisible(map)
            }
            map.addOnCameraIdleListener { loadVisible(map) }
            map.addOnMapClickListener { point ->
                val screen = map.projection.toScreenLocation(point)
                val id = map.queryRenderedFeatures(screen, LAYER_ID)
                    .firstOrNull()
                    ?.getNumberProperty("id")
                    ?.toInt()
                if (id != null) selected = locations[id]
                id != null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text(
                    text = error ?: "Stacje w widoku: ${locations.size}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    selected?.let { loc ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            LocationSheet(
                location = loc,
                onStationClick = { id ->
                    selected = null
                    onStationClick(id)
                },
            )
        }
    }
}

@Composable
private fun LocationSheet(location: Location, onStationClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(location.city ?: "Lokalizacja", style = MaterialTheme.typography.titleLarge)
        location.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        location.region?.name?.let { Text("Województwo: $it", style = MaterialTheme.typography.bodySmall) }
        Text(
            "Stacje: ${location.stations.size}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        location.stations.forEach { st ->
            TextButton(onClick = { onStationClick(st.id) }) {
                Text("${st.stationId}  •  ${st.operator?.name ?: "—"}  •  ${st.status ?: ""}")
            }
        }
        Text(
            "Lokalizacja: ${"%.5f".format(location.latitude)}, ${"%.5f".format(location.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
    }
}
