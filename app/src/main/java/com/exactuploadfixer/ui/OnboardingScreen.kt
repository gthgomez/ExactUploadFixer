package com.exactuploadfixer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.exactuploadfixer.R
import com.workspace.design.AppPrimaryButton
import com.workspace.design.GlassCard
import com.workspace.design.GlassSurfaceStyle
import com.workspace.design.GlassTint

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(1) }

    val slideData = listOf(
        Slide(
            icon = Icons.Default.AddPhotoAlternate,
            title = stringResource(R.string.onboarding_title_1),
            description = stringResource(R.string.onboarding_subtitle_1)
        ),
        Slide(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.onboarding_title_2),
            description = stringResource(R.string.onboarding_subtitle_2)
        ),
        Slide(
            icon = Icons.Default.CloudUpload,
            title = stringResource(R.string.onboarding_title_3),
            description = stringResource(R.string.onboarding_subtitle_3)
        )
    )

    val currentData = slideData[currentSlide - 1]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .tabletContentWidth()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                surfaceStyle = GlassSurfaceStyle.Standard,
                tint = GlassTint.Neutral
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedContent(
                        targetState = currentData,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "slide_transition"
                    ) { slide ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(bottom = 8.dp)
                            )

                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = slide.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Slide Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slideData.forEachIndexed { index, _ ->
                    val isActive = index + 1 == currentSlide
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Actions
            AppPrimaryButton(
                text = if (currentSlide < 3) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_get_started),
                onClick = {
                    if (currentSlide < 3) {
                        currentSlide += 1
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (currentSlide < 3) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
                    )
                }
            } else {
                Spacer(Modifier.height(48.dp)) // Maintain spacing height consistency
            }
        }
    }
}

private data class Slide(
    val icon: ImageVector,
    val title: String,
    val description: String
)
