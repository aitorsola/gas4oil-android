package com.aitorsola.gas4oil

import android.location.Location

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val brand: String = "",
    val model: String = "",
    val capacity: String = "",
    val fuel: FuelType = FuelType.GAS95
) {
    val capacityLitres: Double?
        get() = capacity.replace(',', '.').toDoubleOrNull()

    val isValid: Boolean
        get() = capacityLitres?.let { it > 0 && it <= 500 } == true

    val displayName: String
        get() = listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ")
}

data class FillCost(
    val cheapest: Double,
    val priciest: Double,
    val cheapestStation: Station
)

const val FILL_NEARBY_RADIUS_M = 50_000f
const val FILL_MIN_CANDIDATES = 20

fun fillCandidates(stations: List<Station>, here: Location?): List<Station> {
    if (here == null) return stations
    val result = FloatArray(1)
    val byDistance = stations.map { station ->
        Location.distanceBetween(
            here.latitude, here.longitude, station.latitude, station.longitude, result
        )
        station to result[0]
    }.sortedBy { it.second }
    val near = byDistance.filter { it.second <= FILL_NEARBY_RADIUS_M }
    return (if (near.isEmpty()) byDistance.take(FILL_MIN_CANDIDATES) else near).map { it.first }
}

fun Vehicle.fillCost(stations: List<Station>): FillCost? {
    val litres = capacityLitres ?: return null
    if (litres <= 0) return null
    val priced = stations.mapNotNull { s -> s.price(fuel)?.let { s to it } }
    if (priced.isEmpty()) return null
    val min = priced.minBy { it.second }
    val max = priced.maxBy { it.second }
    return FillCost(min.second * litres, max.second * litres, min.first)
}
