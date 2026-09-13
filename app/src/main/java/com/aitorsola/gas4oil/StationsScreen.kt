package com.aitorsola.gas4oil

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    viewModel: StationsViewModel,
    state: StationsUiState,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (state.isLoaded && !state.needsCityChoice && !state.needsCountryChoice) {
                FloatingActionButton(
                    onClick = onRequestLocation,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.MyLocation, stringResource(R.string.listview_city_uselocation))
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.needsCountryChoice) stringResource(R.string.app_name)
                        else state.city?.replaceFirstChar { it.uppercase() }
                            ?: stringResource(R.string.listview_title_all),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (!state.needsCountryChoice) {
                        CountryMenu(viewModel, state)
                        if (state.brandOptions.isNotEmpty()) BrandMenu(viewModel, state)
                        FuelSortMenu(viewModel, state)
                    }
                    ThemeMenu(viewModel, state)
                }
            )
        }
    ) { inner ->
    Column(
        Modifier
            .padding(inner)
            .fillMaxSize()
    ) {
        if (!(state.isLoaded && state.needsCityChoice) && !state.needsCountryChoice) {
            SearchField(query, onQueryChange = { query = it }, viewModel = viewModel)
        }
        when {
            state.needsCountryChoice -> CountryPrompt(viewModel, state, onRequestLocation)

            state.isPreparing || state.isLoading ->
                LazyColumn(Modifier.fillMaxSize()) { items(4) { SkeletonRow() } }

            state.needsCityChoice -> CityPrompt(viewModel, state, onRequestLocation)

            state.stations.isEmpty() -> EmptyState(state, viewModel)

            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        viewModel.reload()
                        refreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
              LazyColumn(Modifier.fillMaxSize()) {
                val vehicle = state.vehicle
                val cost = if (vehicle != null && vehicle.isValid)
                    vehicle.fillCost(viewModel.fillCandidates()) else null
                if (cost != null) {
                    item(key = "fillCost") {
                        FillCostCard(
                            cost,
                            viewModel.distanceTo(cost.cheapestStation),
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
                items(state.stations, key = { it.id }) { station ->
                    StationRow(
                        station = station,
                        fuel = state.fuel,
                        isFavourite = state.favourites.any { it.id == station.id },
                        viewModel = viewModel
                    )
                    HorizontalDivider()
                }
              }
            }
        }
    }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    viewModel: StationsViewModel
) {
    var suggestions by remember { mutableStateOf(emptyList<String>()) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                suggestions = if (it.isBlank()) emptyList() else viewModel.searchResults(it).take(6)
                if (it.isBlank()) viewModel.showCity("")
            },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text(stringResource(R.string.listview_search_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        )
        suggestions.forEach { town ->
            Text(
                town.replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onQueryChange(town.replaceFirstChar { it.uppercase() })
                        suggestions = emptyList()
                        viewModel.showCity(town)
                    }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun CountryPrompt(
    viewModel: StationsViewModel,
    state: StationsUiState,
    onRequestLocation: () -> Unit
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                Modifier
                    .padding(top = 28.dp)
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Public, null,
                    Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.listview_country_prompt),
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.listview_country_hint),
                fontSize = 14.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(28.dp))
        }
        items(Country.entries) { country ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { viewModel.showCountry(country) }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(country.flag, fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
                Text(
                    stringResource(country.nameRes),
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRequestLocation) {
                Icon(Icons.Filled.MyLocation, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.listview_city_uselocation))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CityPrompt(
    viewModel: StationsViewModel,
    state: StationsUiState,
    onRequestLocation: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, state.municipios) {
        if (query.isBlank()) emptyList() else viewModel.searchResults(query).take(8)
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                Modifier
                    .padding(top = 28.dp)
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocationOn, null,
                    Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.listview_city_prompt),
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.listview_city_hint),
                fontSize = 14.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, null) }
                    }
                },
                placeholder = { Text(stringResource(R.string.listview_search_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isBlank()) {
            item {
                Spacer(Modifier.height(26.dp))
                Text(
                    stringResource(R.string.listview_city_suggested),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.suggestedCities.forEach { town ->
                        Box(
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(22.dp)
                                )
                                .clickable { viewModel.showCity(town) }
                                .padding(horizontal = 18.dp, vertical = 11.dp)
                        ) {
                            Text(
                                town.replaceFirstChar { it.uppercase() },
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp, maxLines = 1
                            )
                        }
                    }
                }
            }
        } else if (matches.isEmpty()) {
            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.listview_city_nomatches),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            items(matches, key = { it }) { town ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showCity(town) }
                        .padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.LocationOn, null,
                        Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(town.replaceFirstChar { it.uppercase() }, fontSize = 16.sp)
                        state.provinceByTown[town]?.let {
                            Text(
                                it.replaceFirstChar { c -> c.uppercase() },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
        }
        item {
            Spacer(Modifier.height(28.dp))
            TextButton(onClick = onRequestLocation) {
                Icon(Icons.Filled.MyLocation, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.listview_city_uselocation))
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EmptyState(state: StationsUiState, viewModel: StationsViewModel) {
    val message = when (state.loadErrorKind) {
        G4OException.Kind.NETWORK -> stringResource(R.string.error_network)
        G4OException.Kind.BAD_STATUS -> stringResource(R.string.error_network)
        G4OException.Kind.EMPTY -> stringResource(R.string.error_emptyresponse)
        G4OException.Kind.PARSE -> stringResource(R.string.error_parse)
        null -> stringResource(R.string.listview_empty)
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, textAlign = TextAlign.Center, fontSize = 18.sp)
        if (state.loadErrorKind != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.load() }) {
                Text(stringResource(R.string.common_retry))
            }
        }
    }
}

@Composable
private fun StationRow(
    station: Station,
    fuel: FuelType,
    isFavourite: Boolean,
    viewModel: StationsViewModel
) {
    val context = LocalContext.current
    val logo = Text.brandTokens(station.rotulo).firstNotNullOfOrNull { StationBrandLogo.from(it) }
    val distance = viewModel.distanceTo(station)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    RoundedCornerShape(15.dp)
                )
                .padding(vertical = 10.dp)
        ) {
            val premiumLabel = stringResource(R.string.fuel_dieselpremium)
            val dieselLabel = stringResource(R.string.fuel_diesel_short)
            val dieselColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            val columns = station.country.fuels.mapNotNull { type ->
                val price = station.rawPrice(type)
                if (price.isBlank()) return@mapNotNull null
                when (type) {
                    FuelType.GAS95 -> Triple("95E5", price, PriceGreen)
                    FuelType.GAS95_PREMIUM -> Triple("95+", price, PriceTeal)
                    FuelType.GAS98 -> Triple("98E5", price, PriceRed)
                    FuelType.DIESEL -> Triple(dieselLabel, price, dieselColor)
                    FuelType.DIESEL_PREMIUM -> Triple(premiumLabel, price, PriceIndigo)
                    FuelType.GLP -> Triple("GLP", price, PriceBlue)
                    FuelType.E10 -> Triple("E10", price, PriceCyan)
                    FuelType.E85 -> Triple("E85", price, PricePurple)
                }
            }
            val size = when {
                columns.size >= 6 -> 10
                columns.size == 5 -> 11
                columns.size == 4 -> 13
                else -> 15
            }
            columns.forEach { (label, price, color) ->
                PriceCell(label, price, color, size, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (logo != null) {
                Image(
                    painterResource(logo.drawable),
                    null,
                    Modifier
                        .size(30.dp)
                        .background(Color.White, CircleShape)
                        .padding(3.dp)
                )
            } else {
                Icon(
                    Icons.Filled.LocalGasStation, null,
                    Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                station.displayTitle.uppercase(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (distance != null) {
                Icon(
                    Icons.Filled.MyLocation, null,
                    Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(formatDistance(distance), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            station.displayAddress,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (station.horario.isNotBlank()) {
            Text(
                station.horario,
                fontSize = 12.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { context.openDirections(station) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Directions, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.listview_station_directions))
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = { viewModel.toggleFavourite(station) }) {
                Icon(
                    if (isFavourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(
                        if (isFavourite) R.string.listview_station_removefavorite
                        else R.string.listview_station_addfavorite
                    ),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PriceCell(
    label: String,
    raw: String,
    color: Color,
    size: Int,
    modifier: Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label, color = color, fontWeight = FontWeight.Bold, fontSize = size.sp,
            maxLines = 1, softWrap = false
        )
        Text(
            "$raw €",
            color = MaterialTheme.colorScheme.primary,
            fontSize = (size + 2).sp,
            maxLines = 1, softWrap = false
        )
    }
}

@Composable
private fun CountryMenu(viewModel: StationsViewModel, state: StationsUiState) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) {
        Text(state.country.flag + " " + state.country.code, color = MaterialTheme.colorScheme.primary)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        Country.entries.forEach { country ->
            DropdownMenuItem(
                text = { Text(country.flag + " " + stringResource(country.nameRes)) },
                leadingIcon = { if (state.country == country) Icon(Icons.Filled.Check, null) },
                onClick = { viewModel.showCountry(country); open = false }
            )
        }
    }
}

@Composable
private fun BrandMenu(viewModel: StationsViewModel, state: StationsUiState) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.listview_brand_all)) },
            leadingIcon = { if (state.brand == null) Icon(Icons.Filled.Check, null) },
            onClick = { viewModel.showBrand(null); open = false }
        )
        HorizontalDivider()
        state.brandOptions.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.title) },
                leadingIcon = {
                    when {
                        state.brand == option.key -> Icon(Icons.Filled.Check, null)
                        option.logo != null -> Image(
                            painterResource(option.logo.drawable), null,
                            Modifier.size(24.dp).background(Color.White, CircleShape).padding(2.dp)
                        )
                        else -> Icon(Icons.Filled.LocalGasStation, null)
                    }
                },
                onClick = { viewModel.showBrand(option.key); open = false }
            )
        }
    }
}

@Composable
private fun FuelSortMenu(viewModel: StationsViewModel, state: StationsUiState) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary)
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        Text(
            stringResource(R.string.common_fueltype),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        state.country.fuels.forEach { fuel ->
            DropdownMenuItem(
                text = { Text(stringResource(fuel.labelRes)) },
                leadingIcon = {
                    Icon(
                        if (state.fuel == fuel) Icons.Filled.Check else Icons.Filled.WaterDrop,
                        null
                    )
                },
                onClick = { viewModel.showFuel(fuel); open = false }
            )
        }
        HorizontalDivider()
        Text(
            stringResource(R.string.listview_sort_title),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        StationSort.entries.forEach { sort ->
            val label = when (sort) {
                StationSort.NEAREST -> R.string.listview_sortorder_near
                StationSort.CHEAPEST -> R.string.listview_sortorder_down
                StationSort.PRICIEST -> R.string.listview_sortorder_up
            }
            val icon = when (sort) {
                StationSort.NEAREST -> Icons.Filled.MyLocation
                StationSort.CHEAPEST -> Icons.Filled.ArrowDownward
                StationSort.PRICIEST -> Icons.Filled.ArrowUpward
            }
            DropdownMenuItem(
                text = { Text(stringResource(label)) },
                leadingIcon = { Icon(if (state.sort == sort) Icons.Filled.Check else icon, null) },
                onClick = { viewModel.showSort(sort); open = false }
            )
        }
    }
}

@Composable
private fun ThemeMenu(viewModel: StationsViewModel, state: StationsUiState) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(
            when (state.theme) {
                ThemePreference.SYSTEM -> Icons.Filled.Brightness6
                ThemePreference.LIGHT -> Icons.Filled.LightMode
                ThemePreference.DARK -> Icons.Filled.DarkMode
            },
            contentDescription = stringResource(R.string.appearance_title),
            tint = MaterialTheme.colorScheme.primary
        )
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        ThemePreference.entries.forEach { option ->
            val label = when (option) {
                ThemePreference.SYSTEM -> R.string.appearance_system
                ThemePreference.LIGHT -> R.string.appearance_light
                ThemePreference.DARK -> R.string.appearance_dark
            }
            val icon = when (option) {
                ThemePreference.SYSTEM -> Icons.Filled.Brightness6
                ThemePreference.LIGHT -> Icons.Filled.LightMode
                ThemePreference.DARK -> Icons.Filled.DarkMode
            }
            DropdownMenuItem(
                text = { Text(stringResource(label)) },
                leadingIcon = {
                    Icon(if (state.theme == option) Icons.Filled.Check else icon, null)
                },
                onClick = { viewModel.setTheme(option); open = false }
            )
        }
    }
}
