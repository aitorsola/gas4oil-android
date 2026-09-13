package com.aitorsola.gas4oil

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    viewModel: StationsViewModel,
    state: StationsUiState,
    modifier: Modifier = Modifier
) {
    val fuel = state.vehicle?.fuel ?: FuelType.GAS95
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.favorites_title), fontWeight = FontWeight.Bold)
            })
        }
    ) { inner ->
    if (state.favourites.isEmpty()) {
        Column(
            Modifier.padding(inner).fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.favorites_empty), fontSize = 18.sp)
        }
        return@Scaffold
    }
    val context = LocalContext.current
    LazyColumn(Modifier.padding(inner).fillMaxSize()) {
        items(state.favourites, key = { it.id }) { station ->
            val logo = Text.brandTokens(station.rotulo)
                .firstNotNullOfOrNull { StationBrandLogo.from(it) }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (logo != null) {
                    Image(
                        painterResource(logo.drawable), null,
                        Modifier.size(34.dp).background(Color.White, CircleShape).padding(3.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.LocalGasStation, null,
                        Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(station.displayTitle.uppercase(), fontWeight = FontWeight.Bold)
                    Text(
                        station.displayAddress,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        station.municipio.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val raw = station.rawPrice(fuel)
                    Text(
                        if (raw.isBlank()) "--" else "$raw €",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { context.openDirections(station) }) {
                            Icon(
                                Icons.Filled.Directions,
                                contentDescription = stringResource(R.string.listview_station_directions),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavourite(station) }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = stringResource(R.string.listview_station_removefavorite),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
        }
    }
    }
}
