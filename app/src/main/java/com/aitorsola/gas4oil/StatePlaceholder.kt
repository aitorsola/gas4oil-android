package com.aitorsola.gas4oil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatePlaceholder(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(84.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                message, fontSize = 14.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        if (actionTitle != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(actionTitle) }
        }
    }
}

@Composable
fun ErrorPlaceholder(kind: G4OException.Kind, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    val (icon, title, message) = when (kind) {
        G4OException.Kind.OFFLINE ->
            Triple(Icons.Filled.WifiOff, R.string.error_offline_title, R.string.error_offline_message)
        G4OException.Kind.NETWORK, G4OException.Kind.BAD_STATUS ->
            Triple(Icons.Filled.CloudOff, R.string.error_service_title, R.string.error_network)
        G4OException.Kind.EMPTY ->
            Triple(Icons.Filled.ErrorOutline, R.string.error_data_title, R.string.error_emptyresponse)
        G4OException.Kind.PARSE ->
            Triple(Icons.Filled.ErrorOutline, R.string.error_data_title, R.string.error_parse)
    }
    StatePlaceholder(
        icon = icon,
        title = stringResource(title),
        modifier = modifier,
        message = stringResource(message),
        actionTitle = stringResource(R.string.common_retry),
        onAction = onRetry
    )
}
