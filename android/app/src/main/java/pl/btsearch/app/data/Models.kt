package pl.btsearch.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataResponse<T>(val data: T)

@Serializable
data class LocationsResponse(
    val data: List<Location> = emptyList(),
    val totalCount: Int = 0,
)

@Serializable
data class Region(
    val name: String? = null,
    val code: String? = null,
)

@Serializable
data class Operator(
    val id: Int? = null,
    val name: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("short_code") val shortCode: String? = null,
)

@Serializable
data class StationSummary(
    val id: Int,
    @SerialName("station_id") val stationId: String = "",
    val status: String? = null,
    val operator: Operator? = null,
)

@Serializable
data class Location(
    val id: Int,
    val city: String? = null,
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val region: Region? = null,
    val stations: List<StationSummary> = emptyList(),
)

@Serializable
data class Band(
    val name: String? = null,
    val value: Int? = null,
    val rat: String? = null,
)

@Serializable
data class Cell(
    val id: Int,
    val rat: String? = null,
    val type: String? = null,
    val notes: String? = null,
    val band: Band? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null,
)

@Serializable
data class StationDetail(
    val id: Int,
    @SerialName("station_id") val stationId: String = "",
    val status: String? = null,
    val notes: String? = null,
    @SerialName("extra_address") val extraAddress: String? = null,
    val location: Location? = null,
    val operator: Operator? = null,
    val cells: List<Cell> = emptyList(),
)

class ApiException(val code: Int, message: String) : Exception(message)
