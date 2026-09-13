package com.aitorsola.gas4oil

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StationsUiState(
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val loadErrorKind: G4OException.Kind? = null,
    val stations: List<Station> = emptyList(),
    val municipios: List<String> = emptyList(),
    val brandOptions: List<BrandOption> = emptyList(),
    val favourites: List<Station> = emptyList(),
    val country: Country = Country.SPAIN,
    val hasChosenCountry: Boolean = false,
    val isLocating: Boolean = false,
    val fuel: FuelType = FuelType.GAS95,
    val sort: StationSort = StationSort.NEAREST,
    val brand: String? = null,
    val city: String? = null,
    val locationDenied: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val vehicle: Vehicle? = null,
    val hasCoordinates: Boolean = false,
    val provinceByTown: Map<String, String> = emptyMap()
) {
    val isPreparing: Boolean get() = stations.isEmpty() && !isLoaded

    val needsCityChoice: Boolean get() = !hasCoordinates && city == null

    val needsCountryChoice: Boolean get() = !hasChosenCountry && !isLocating

    val suggestedCities: List<String>
        get() = country.suggestedCities.filter { municipios.contains(it) }
}

class StationsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val locationProvider = LocationProvider(app)

    private val savedCountry = prefs.country
    private val startCountry = savedCountry ?: Country.SPAIN
    private var countryPinnedByUser = false
    private var detectedCity: String? = null
    private var detectedCountry: Country? = null

    private fun locationTitle(country: Country): String? {
        if (coordinates == null) return null
        if (detectedCountry != country) {
            return getApplication<Application>().getString(country.nameRes)
        }
        return detectedCity
    }

    private val _state = MutableStateFlow(
        StationsUiState(
            country = startCountry,
            hasChosenCountry = savedCountry != null,
            fuel = prefs.fuel.takeIf { startCountry.fuels.contains(it) } ?: startCountry.defaultFuel,
            sort = prefs.sort,
            brand = prefs.brand,
            theme = prefs.theme,
            city = prefs.city?.replaceFirstChar { it.uppercase() },
            favourites = prefs.favourites,
            vehicle = prefs.vehicle
        )
    )
    val state: StateFlow<StationsUiState> = _state.asStateFlow()

    private var allStations: List<Station> = emptyList()
    private var townKeys: List<String> = emptyList()
    private var searchKeys: Map<Int, String> = emptyMap()
    private var coordinates: Location? = null
    private var searchCity: String? = null
    private var fetchJob: Job? = null

    fun start() {
        if (_state.value.hasChosenCountry && allStations.isEmpty() && fetchJob?.isActive != true) load()
        resolveLocation()
    }

    fun load() {
        fetchJob?.cancel()
        _state.update { it.copy(isLoading = true, loadErrorKind = null) }
        val country = _state.value.country
        fetchJob = viewModelScope.launch {
            try {
                val downloaded = StationsApi.allStations(country)
                adopt(downloaded)
            } catch (e: G4OException) {
                _state.update {
                    it.copy(isLoading = false, isLoaded = true, loadErrorKind = e.kind)
                }
            }
        }
    }

    fun locationRevoked() {
        coordinates = null
        detectedCity = null
        detectedCountry = null
        _state.update {
            it.copy(
                locationDenied = true, hasCoordinates = false, isLocating = false,
                city = prefs.city?.replaceFirstChar { c -> c.uppercase() }
            )
        }
        refresh()
    }

    fun resolveLocation() {
        countryPinnedByUser = false
        if (_state.value.isLocating) return
        viewModelScope.launch {
            if (!locationProvider.hasPermission) {
                val askCountry = prefs.city == null && allStations.isEmpty()
                _state.update {
                    it.copy(locationDenied = true, hasChosenCountry = it.hasChosenCountry && !askCountry)
                }
                return@launch
            }
            _state.update { it.copy(isLocating = true) }
            val place = locationProvider.current()
            if (place == null) {
                _state.update { it.copy(locationDenied = true, isLocating = false) }
                return@launch
            }
            coordinates = place.location
            detectedCity = place.city
            detectedCountry = Country.fromIso(place.countryCode)
            searchCity = null
            prefs.city = null
            val detected = detectedCountry
            val current = _state.value.country
            _state.update {
                it.copy(
                    locationDenied = false, hasCoordinates = true,
                    city = locationTitle(current), isLocating = false
                )
            }
            if (detected != null && !countryPinnedByUser) {
                applyCountry(detected)
            }
            refresh()
        }
    }

    fun showCountry(country: Country) {
        countryPinnedByUser = true
        applyCountry(country)
    }

    private fun applyCountry(country: Country) {
        prefs.country = country
        val current = _state.value
        val firstChoice = !current.hasChosenCountry
        if (country == current.country) {
            _state.update { it.copy(hasChosenCountry = true) }
            if (allStations.isEmpty() && fetchJob?.isActive != true) load()
            return
        }
        val fuel = when {
            firstChoice && !prefs.hasFuel -> country.defaultFuel
            country.fuels.contains(current.fuel) -> current.fuel
            else -> country.defaultFuel
        }
        prefs.fuel = fuel
        prefs.brand = null
        prefs.city = null
        searchCity = null
        _state.update {
            it.copy(
                country = country, hasChosenCountry = true, fuel = fuel,
                brand = null, city = if (coordinates == null) null else locationTitle(country),
                stations = emptyList()
            )
        }
        allStations = emptyList()
        load()
    }

    private suspend fun adopt(downloaded: List<Station>) {
        val derived = withContext(Dispatchers.Default) {
            val municipios = downloaded.map { it.municipio }.distinct().sorted()
            Triple(
                municipios,
                municipios.map { Text.searchNormalized(Text.townSearchKey(it)) },
                downloaded.associate {
                    it.id to Text.searchNormalized(
                        Text.townSearchKey(it.municipio) + " " + it.provincia
                    )
                }
            )
        }
        allStations = downloaded
        prefs.city?.let { saved ->
            if (coordinates == null && searchCity == null) {
                searchCity = Text.searchNormalized(saved)
            }
        }
        townKeys = derived.second
        searchKeys = derived.third
        val options = withContext(Dispatchers.Default) { Text.brandOptions(downloaded) }
        val savedBrand = _state.value.brand
        val brand = if (savedBrand != null && options.none { it.key == savedBrand }) {
            prefs.brand = null
            null
        } else savedBrand
        val refreshedFavourites = refreshFavouritePrices(downloaded)
        _state.update {
            it.copy(
                isLoading = false,
                isLoaded = true,
                loadErrorKind = null,
                municipios = derived.first,
                provinceByTown = downloaded.associate { it.municipio to it.provincia },
                brandOptions = options,
                favourites = refreshedFavourites,
                brand = brand
            )
        }
        refresh()
    }

    private fun refreshFavouritePrices(downloaded: List<Station>): List<Station> {
        val stored = prefs.favourites
        if (stored.isEmpty()) return stored
        val byId = downloaded.associateBy { it.id }
        val updated = stored.map { fav ->
            byId[fav.id]?.let {
                fav.copy(
                    gasoleoA = it.gasoleoA,
                    gasolina95E5 = it.gasolina95E5,
                    gasolina98E5 = it.gasolina98E5,
                    glp = it.glp,
                    gasoleoPremium = it.gasoleoPremium,
                    gasolina95E5Premium = it.gasolina95E5Premium
                )
            } ?: fav
        }
        prefs.favourites = updated
        return updated
    }

    fun searchResults(text: String): List<String> {
        val needle = Text.searchNormalized(text)
        val all = _state.value.municipios
        if (needle.isEmpty()) return all
        return all.indices.filter { townKeys[it].contains(needle) }.map { all[it] }
    }

    fun showCity(city: String) {
        val trimmed = city.trim()
        searchCity = Text.searchNormalized(trimmed).ifEmpty { null }
        if (coordinates == null) {
            prefs.city = searchCity?.let { trimmed }
        }
        _state.update {
            it.copy(
                city = if (searchCity == null && coordinates == null) null
                else if (searchCity == null) locationTitle(it.country)
                else trimmed.replaceFirstChar { c -> c.uppercase() }
            )
        }
        refresh()
    }

    fun showFuel(fuel: FuelType) {
        prefs.fuel = fuel
        _state.update { it.copy(fuel = fuel) }
        refresh()
    }

    fun showSort(sort: StationSort) {
        prefs.sort = sort
        _state.update { it.copy(sort = sort) }
        refresh()
    }

    fun showBrand(key: String?) {
        prefs.brand = key
        _state.update { it.copy(brand = key) }
        refresh()
    }

    fun setTheme(theme: ThemePreference) {
        prefs.theme = theme
        _state.update { it.copy(theme = theme) }
    }

    fun saveVehicle(vehicle: Vehicle) {
        prefs.vehicle = vehicle
        _state.update { it.copy(vehicle = vehicle) }
    }

    fun removeVehicle() {
        prefs.vehicle = null
        _state.update { it.copy(vehicle = null) }
    }

    fun toggleFavourite(station: Station) {
        val current = prefs.favourites.toMutableList()
        if (current.any { it.id == station.id }) current.removeAll { it.id == station.id }
        else current.add(station)
        prefs.favourites = current
        _state.update { it.copy(favourites = current) }
    }

    fun fillCandidates(): List<Station> = fillCandidates(_state.value.stations, coordinates)

    suspend fun reload() {
        load()
        fetchJob?.join()
        if (locationProvider.hasPermission && coordinates == null) resolveLocation()
    }

    fun distanceTo(station: Station): Float? {
        val from = coordinates ?: return null
        val result = FloatArray(1)
        Location.distanceBetween(
            from.latitude, from.longitude, station.latitude, station.longitude, result
        )
        return result[0]
    }

    private fun refresh() {
        val fuel = _state.value.fuel
        val brand = _state.value.brand
        val city = searchCity
        val filtered = allStations.filter { station ->
            (city == null || searchKeys[station.id]?.contains(city) == true) &&
                (brand == null || Text.brandMatches(station.rotulo, brand)) &&
                station.price(fuel) != null
        }
        val here = coordinates
        val capped = if (here == null) {
            sortedByPrice(filtered, fuel).take(MAX_RESULTS)
        } else {
            val result = FloatArray(1)
            val byDistance = filtered.sortedBy { station ->
                Location.distanceBetween(
                    here.latitude, here.longitude, station.latitude, station.longitude, result
                )
                result[0]
            }
            sortedByPrice(byDistance.take(MAX_RESULTS), fuel)
        }
        _state.update { it.copy(stations = capped) }
    }

    private fun sortedByPrice(stations: List<Station>, fuel: FuelType) = when (_state.value.sort) {
        StationSort.NEAREST -> stations
        StationSort.CHEAPEST -> stations.sortedBy { it.price(fuel) ?: Double.MAX_VALUE }
        StationSort.PRICIEST -> stations.sortedByDescending { it.price(fuel) ?: 0.0 }
    }

    private companion object {
        const val MAX_RESULTS = 200
    }
}
