package com.markduenas.scorekeeper.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import scorekeeper.composeapp.generated.resources.Res
import scorekeeper.composeapp.generated.resources.scorr_icon
import androidx.compose.foundation.Image

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(600))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E2A)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.scorr_icon),
            contentDescription = "Scorr",
            modifier = Modifier
                .size(180.dp)
                .alpha(alpha.value)
        )
    }
}
