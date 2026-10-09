package pl.btsearch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pl.btsearch.app.data.BtsApi
import pl.btsearch.app.ui.AppRoot
import pl.btsearch.app.ui.theme.BTSearchTheme

class MainActivity : ComponentActivity() {
    private val api by lazy { BtsApi(BuildConfig.API_BASE_URL) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BTSearchTheme {
                AppRoot(api = api)
            }
        }
    }
}
