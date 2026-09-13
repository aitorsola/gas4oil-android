package com.aitorsola.gas4oil

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Prefs(context: Context) {

    private val store: SharedPreferences =
        context.getSharedPreferences("gas4oil", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    var fuel: FuelType
        get() = FuelType.from(store.getString(KEY_FUEL, null)) ?: FuelType.GAS95
        set(value) = store.edit().putString(KEY_FUEL, value.storageKey).apply()

    var sort: StationSort
        get() = StationSort.from(store.getString(KEY_SORT, null)) ?: StationSort.NEAREST
        set(value) = store.edit().putString(KEY_SORT, value.storageKey).apply()

    var brand: String?
        get() = store.getString(KEY_BRAND, null)
        set(value) =
            if (value == null) store.edit().remove(KEY_BRAND).apply()
            else store.edit().putString(KEY_BRAND, value).apply()

    var city: String?
        get() = store.getString(KEY_CITY, null)
        set(value) =
            if (value == null) store.edit().remove(KEY_CITY).apply()
            else store.edit().putString(KEY_CITY, value).apply()

    var country: Country?
        get() = store.getString(KEY_COUNTRY, null)?.let { Country.from(it) }
        set(value) =
            if (value == null) store.edit().remove(KEY_COUNTRY).apply()
            else store.edit().putString(KEY_COUNTRY, value.storageKey).apply()

    val hasFuel: Boolean get() = store.contains(KEY_FUEL)

    var theme: ThemePreference
        get() = ThemePreference.from(store.getString(KEY_THEME, null))
        set(value) = store.edit().putString(KEY_THEME, value.storageKey).apply()

    var favourites: List<Station>
        get() = store.getString(KEY_FAVS, null)?.let {
            runCatching { json.decodeFromString<List<Station>>(it) }.getOrNull()
        } ?: emptyList()
        set(value) = store.edit().putString(KEY_FAVS, json.encodeToString(value)).apply()

    var vehicle: Vehicle?
        get() = store.getString(KEY_VEHICLE, null)?.let {
            runCatching { json.decodeFromString<Vehicle>(it) }.getOrNull()
        }
        set(value) =
            if (value == null) store.edit().remove(KEY_VEHICLE).apply()
            else store.edit().putString(KEY_VEHICLE, json.encodeToString(value)).apply()

    private companion object {
        const val KEY_FUEL = "listView.selectedFuel"
        const val KEY_SORT = "listView.sortOrder"
        const val KEY_BRAND = "listView.brand"
        const val KEY_CITY = "listView.city"
        const val KEY_COUNTRY = "listView.country"
        const val KEY_THEME = "appearance"
        const val KEY_FAVS = "favs"
        const val KEY_VEHICLE = "vehicle"
    }
}
