package pl.btsearch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pl.btsearch.app.data.BtsApi
import pl.btsearch.app.data.Location

private data class StationRow(val stationId: Int, val code: String, val operator: String, val city: String, val status: String?)

@Composable
fun SearchScreen(api: BtsApi, onStationClick: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<StationRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    fun search() {
        if (query.isBlank()) return
        scope.launch {
            loading = true
            error = null
            try {
                val response = api.locations(
                    south = -90.0, west = -180.0, north = 90.0, east = 180.0,
                    query = query, limit = 100,
                )
                rows = response.data.flatMap { loc: Location ->
                    loc.stations.map { st ->
                        StationRow(
                            stationId = st.id,
                            code = st.stationId,
                            operator = st.operator?.name ?: "—",
                            city = listOfNotNull(loc.city, loc.address).joinToString(", "),
                            status = st.status,
                        )
                    }
                }
                searched = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Błąd wyszukiwania"
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Kod stacji, miasto lub adres") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = { search() }, enabled = query.isNotBlank() && !loading) { Text("Szukaj") }
        }

        if (loading) CircularProgressIndicator(modifier = Modifier.padding(vertical = 12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }

        if (searched && rows.isEmpty() && !loading && error == null) {
            Text("Brak wyników.", modifier = Modifier.padding(vertical = 12.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rows, key = { it.stationId }) { row ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text("${row.code}  •  ${row.operator}", style = MaterialTheme.typography.titleMedium)
                    Text(row.city, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Status: ${row.status ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { onStationClick(row.stationId) }, modifier = Modifier.padding(top = 6.dp)) {
                        Text("Szczegóły")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
