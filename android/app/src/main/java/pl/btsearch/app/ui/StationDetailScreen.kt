package pl.btsearch.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import pl.btsearch.app.data.BtsApi
import pl.btsearch.app.data.StationDetail

@Composable
fun StationDetailScreen(api: BtsApi, stationId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var station by remember { mutableStateOf<StationDetail?>(null) }
    var permits by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(stationId) {
        loading = true
        error = null
        try {
            station = api.station(stationId)
            permits = runCatching { api.permits(stationId) }.getOrDefault(emptyList())
        } catch (e: Exception) {
            error = e.message ?: "Nie udało się pobrać stacji"
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedButton(onClick = onBack) { Text("← Wróć") }

        if (loading) CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }

        val st = station ?: return@Column
        Text(
            st.stationId,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text("Operator: ${st.operator?.fullName ?: st.operator?.name ?: "—"}")
        Text("Status: ${st.status ?: "—"}")
        st.location?.let { loc ->
            Text("Miasto: ${loc.city ?: "—"}")
            Text("Adres: ${loc.address ?: "—"}")
            loc.region?.name?.let { Text("Województwo: $it") }
        }
        st.extraAddress?.let { Text("Dodatkowy adres: $it") }
        st.notes?.takeIf { it.isNotBlank() }?.let { Text("Uwagi: $it", modifier = Modifier.padding(top = 4.dp)) }

        st.location?.let { loc ->
            if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                Button(
                    onClick = {
                        val uri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(${st.stationId})")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text("Nawiguj do stacji") }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Komórki (${st.cells.size})", style = MaterialTheme.typography.titleMedium)
        if (st.cells.isEmpty()) Text("Brak danych o komórkach.")
        st.cells.forEach { cell ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${cell.rat?.uppercase() ?: "?"}  ${cell.band?.name ?: ""}")
                Text(cell.type ?: "", style = MaterialTheme.typography.bodySmall)
            }
            cell.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Pozwolenia UKE (${permits.size})", style = MaterialTheme.typography.titleMedium)
        if (permits.isEmpty()) Text("Brak pozwoleń.")
        permits.forEach { p ->
            val title = p.str("number") ?: p.str("name") ?: p.str("id") ?: "Pozwolenie"
            val band = p["band"]?.let { (it as? JsonObject)?.str("name") }
            Text(listOfNotNull(title, band).joinToString("  •  "), modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

private fun JsonObject.str(key: String): String? =
    this[key]?.jsonPrimitive?.contentOrNull
