package com.aitorsola.gas4oil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(
    viewModel: StationsViewModel,
    state: StationsUiState,
    modifier: Modifier = Modifier
) {
    val saved = state.vehicle
    var brand by remember(saved) { mutableStateOf(saved?.brand ?: "") }
    var model by remember(saved) { mutableStateOf(saved?.model ?: "") }
    var capacity by remember(saved) { mutableStateOf(saved?.capacity ?: "") }
    var fuel by remember(saved) { mutableStateOf(saved?.fuel ?: FuelType.GAS95) }
    var fuelMenuOpen by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    val draft = Vehicle(brand, model, capacity, fuel)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.myvehicle_brand_title), fontWeight = FontWeight.Bold)
            })
        }
    ) { inner ->
    Column(
        Modifier
            .padding(inner)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (saved != null) {
            val cost = saved.fillCost(viewModel.fillCandidates())
            Text(
                saved.displayName.ifBlank { stringResource(R.string.myvehicle_unnamed) },
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (cost != null) {
                FillCostCard(cost, viewModel.distanceTo(cost.cheapestStation))
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.myvehicle_fill_unavailable),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.myvehicle_fill_footer),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.myvehicle_ad_title),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.myvehicle_ad_description), fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.myvehicle_section_vehicle), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text(stringResource(R.string.myvehicle_brand_placeholder)) },
            placeholder = { Text(stringResource(R.string.myvehicle_brand_example)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text(stringResource(R.string.myvehicle_model_placeholder)) },
            placeholder = { Text(stringResource(R.string.myvehicle_model_example)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.myvehicle_section_vehicle_hint),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.myvehicle_section_tank), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { fuelMenuOpen = true }) {
                Text(
                    stringResource(
                        fuel.labelRes
                    )
                )
            }
            DropdownMenu(expanded = fuelMenuOpen, onDismissRequest = { fuelMenuOpen = false }) {
                FuelType.entries.forEach { option ->
                    val label = option.labelRes
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        leadingIcon = { if (fuel == option) Icon(Icons.Filled.Check, null) },
                        onClick = { fuel = option; fuelMenuOpen = false }
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = capacity,
                onValueChange = { capacity = it },
                label = { Text(stringResource(R.string.myvehicle_capacity_placeholder)) },
                placeholder = { Text(stringResource(R.string.myvehicle_capacity_example)) },
                suffix = { Text(stringResource(R.string.myvehicle_capacity_unit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (capacity.isNotEmpty() && !draft.isValid)
                stringResource(R.string.myvehicle_capacity_invalid)
            else stringResource(R.string.myvehicle_section_tank_hint),
            fontSize = 12.sp,
            color = if (capacity.isNotEmpty() && !draft.isValid) PriceRed
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.saveVehicle(draft) },
            enabled = draft.isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.myvehicle_save))
        }
        if (saved != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { confirmRemove = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.myvehicle_remove), color = PriceRed)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.myvehicle_remove_alert_title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeVehicle()
                    brand = ""; model = ""; capacity = ""; fuel = FuelType.GAS95
                    confirmRemove = false
                }) { Text(stringResource(R.string.myvehicle_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

internal fun euros(value: Double): String =
    String.format("%.2f", value).replace('.', ',') + " €"
