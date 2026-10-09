package pl.btsearch.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Klient publicznego REST API btsearch (/api/v1).
 * Adres bazowy pochodzi z BuildConfig.API_BASE_URL.
 */
class BtsApi(private val baseUrl: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * Stacje w prostokącie. Granice w formacie wymaganym przez serwer: lat1,lng1,lat2,lng2.
     */
    suspend fun locations(
        south: Double,
        west: Double,
        north: Double,
        east: Double,
        query: String? = null,
        limit: Int = 500,
    ): LocationsResponse {
        val params = mutableMapOf(
            "bounds" to "$south,$west,$north,$east",
            "limit" to limit.toString(),
        )
        if (!query.isNullOrBlank()) params["q"] = query.trim()
        return get("locations", params)
    }

    suspend fun station(id: Int): StationDetail =
        get<DataResponse<StationDetail>>("stations/$id").data

    suspend fun permits(stationId: Int): List<JsonObject> =
        get<DataResponse<List<JsonObject>>>("stations/$stationId/permits").data

    private suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap(),
    ): T = withContext(Dispatchers.IO) {
        val url = baseUrl.trimEnd('/').toHttpUrl().newBuilder().apply {
            path.split('/').forEach { addPathSegment(it) }
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "BTSearch-Android")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, body.take(200))
            json.decodeFromString<T>(body)
        }
    }
}
