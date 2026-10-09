package pl.btsearch.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pl.btsearch.app.BuildConfig

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("BTSearch", style = MaterialTheme.typography.headlineMedium)
        Text("Wersja ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        Text(
            "Mapa stacji bazowych w Polsce wraz z pozwoleniami UKE. Aplikacja korzysta z publicznego API btsearch.",
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Serwer API: ${BuildConfig.API_BASE_URL}",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Licencja: GPL-3.0 (zgodnie z repozytorium źródłowym).", modifier = Modifier.padding(top = 12.dp))

        Button(
            onClick = { context.openUrl("https://github.com/btsearch/btsearch") },
            modifier = Modifier.padding(top = 16.dp),
        ) { Text("Kod źródłowy (GitHub)") }
        Button(
            onClick = { context.openUrl("https://btsearch.pl") },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Strona btsearch.pl") }
    }
}

private fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
