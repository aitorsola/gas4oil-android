package com.aitorsola.gas4oil

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Gas4OilApp() }
    }
}

private enum class Tab { STATIONS, VEHICLE, FAVOURITES }

@Composable
private fun Gas4OilApp() {
    val viewModel: StationsViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.STATIONS) }
    var askedForPermission by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.resolveLocation() }

    val requestLocation: () -> Unit = {
        val activity = context as? Activity
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val canAsk = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        )
        when {
            granted -> viewModel.resolveLocation()
            canAsk || !askedForPermission ->
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            else -> context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.start()
        if (!askedForPermission) {
            askedForPermission = true
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted && !state.hasCoordinates) viewModel.resolveLocation()
        else if (!granted && state.hasCoordinates) viewModel.locationRevoked()
    }

    Gas4OilTheme(state.theme) {
        Surface {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == Tab.STATIONS,
                            onClick = { tab = Tab.STATIONS },
                            icon = { Icon(Icons.Filled.LocalGasStation, null) },
                            label = { Text(stringResource(R.string.maintab_stationstabtitle)) }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.VEHICLE,
                            onClick = { tab = Tab.VEHICLE },
                            icon = { Icon(Icons.Filled.DirectionsCar, null) },
                            label = { Text(stringResource(R.string.maintab_vehicle)) }
                        )
                        NavigationBarItem(
                            selected = tab == Tab.FAVOURITES,
                            onClick = { tab = Tab.FAVOURITES },
                            icon = { Icon(Icons.Filled.Star, null) },
                            label = { Text(stringResource(R.string.maintab_favtabtitle)) }
                        )
                    }
                }
            ) { insets ->
                val outer = Modifier.padding(bottom = insets.calculateBottomPadding())
                when (tab) {
                    Tab.STATIONS -> StationsScreen(viewModel, state, requestLocation, outer)
                    Tab.VEHICLE -> VehicleScreen(viewModel, state, outer)
                    Tab.FAVOURITES -> FavouritesScreen(viewModel, state, outer)
                }
            }
        }
    }
}
