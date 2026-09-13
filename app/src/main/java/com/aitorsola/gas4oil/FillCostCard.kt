package com.aitorsola.gas4oil

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

fun Context.openDirections(station: Station) {
    val uri = Uri.parse(
        "geo:0,0?q=${station.latitude},${station.longitude}(${Uri.encode(station.displayTitle)})"
    )
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

fun formatDistance(metres: Float): String =
    if (metres < 1000) "${metres.toInt()} m"
    else String.format("%.1f km", metres / 1000).replace('.', ',')

fun perLitre(value: Double): String =
    String.format(java.util.Locale.ROOT, "%.3f €/l", value).replace('.', ',')

@Composable
fun FillCostCard(
    station: Station,
    fuel: FuelType,
    pricePerLitre: Double,
    fillCost: Double?,
    distance: Float?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val place = "${station.displayTitle} (${station.municipio.replaceFirstChar { it.uppercase() }})"
    val detail = if (fillCost == null) place else "$place · ${perLitre(pricePerLitre)}"
    val logo = Text.brandTokens(station.rotulo).firstNotNullOfOrNull { StationBrandLogo.from(it) }
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (fillCost == null) stringResource(R.string.listview_cheapest_title, stringResource(fuel.labelRes))
                    else stringResource(R.string.myvehicle_fill_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    fillCost?.let { euros(it) } ?: perLitre(pricePerLitre),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = PriceGreen
                )
            }
            if (logo != null) {
                Image(
                    painterResource(logo.drawable), null,
                    Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                        .padding(5.dp)
                )
            } else {
                Icon(
                    Icons.Filled.LocalGasStation, null,
                    Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                detail,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
            if (distance != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.NearMe, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    formatDistance(distance),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { context.openDirections(station) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Directions, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.listview_station_directions))
        }
    }
}
