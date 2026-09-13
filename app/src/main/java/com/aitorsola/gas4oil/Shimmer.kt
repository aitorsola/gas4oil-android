package com.aitorsola.gas4oil

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun shimmerModifier(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "sweep"
    )
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    return Modifier.drawWithContent {
        drawContent()
        val span = size.width * 1.6f
        val x = -span + (size.width + span) * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight, Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + span * 0.45f, size.height)
            )
        )
    }
}

@Composable
fun SkeletonRow() {
    val fill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f)

    @Composable
    fun bar(width: Int, height: Int) {
        Box(
            Modifier
                .width(width.dp)
                .height(height.dp)
                .background(fill, RoundedCornerShape(height.dp / 2))
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .then(shimmerModifier())
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(fill, RoundedCornerShape(15.dp))
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Box(Modifier.size(30.dp).background(fill, CircleShape))
            Spacer(Modifier.width(10.dp))
            bar(110, 18)
        }
        Spacer(Modifier.height(12.dp))
        bar(190, 13)
        Spacer(Modifier.height(8.dp))
        bar(130, 11)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(fill, RoundedCornerShape(12.dp))
        )
    }
}
