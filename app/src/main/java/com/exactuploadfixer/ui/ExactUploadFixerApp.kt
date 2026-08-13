package com.exactuploadfixer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.exactuploadfixer.R
import com.workspace.design.AppTheme

private val TabletContentMaxWidth = 560.dp

/**
 * Root Compose entry point. Three-screen switch driven by AppUiState.screen.
 *
 * No NavHost, no back-stack — the flow is linear and ViewModel controls all
 * navigation. This keeps navigation logic out of Compose and testable in the ViewModel.
 *
 * Source: ChatGPT Step 4 "state-driven UI switching" pattern.
 */
@Composable
fun ExactUploadFixerApp(vm: MainViewModel) {
    AppTheme {
        val ui = vm.uiState

        when (ui.screen) {
            AppScreen.Onboarding -> OnboardingScreen(
                onComplete = vm::onOnboardingCompleted
            )

            AppScreen.Pick -> PickScreen(
                onPhotoPicked = vm::onPhotoPicked,
                pickError = ui.pickError
            )

            AppScreen.Edit -> EditScreen(
                ui = ui,
                onMaxSizeChanged = vm::onMaxSizeChanged,
                onWidthChanged = vm::onWidthChanged,
                onHeightChanged = vm::onHeightChanged,
                onPresetSelected = vm::onPresetSelected,
                onEntitlementRefreshRequested = vm::refreshBillingEntitlement,
                onProcessClick = vm::onProcessClick,
                onBuyProClick = vm::buyPro,
                onBack = vm::onStartOver
            )

            AppScreen.Result -> ResultScreen(
                ui = ui,
                onBackToEdit = vm::onBackToEdit,
                onStartOver = vm::onStartOver
            )
        }
    }
}

/**
 * Visual step indicator shown on screens 2 and 3.
 * currentStep: 1 = Pick, 2 = Fix, 3 = Ready
 *
 * REFINED LOGIC:
 * - Completed: Filled check icon + solid line
 * - Current: Primary outline + number + solid line (if index > 0)
 * - Upcoming: Gray outline + number + dashed line
 */
@Composable
internal fun StepIndicator(currentStep: Int) {
    val steps = listOf(
        stringResource(R.string.step_pick),
        stringResource(R.string.step_set),
        stringResource(R.string.step_ready)
    )
    val stateDescription = stringResource(R.string.step_indicator_cd, currentStep)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { this.stateDescription = stateDescription },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isDone = stepNumber < currentStep
            val isCurrent = stepNumber == currentStep
            val isUpcoming = stepNumber > currentStep

            // Line before (except first)
            if (index > 0) {
                val isUpcomingLine = stepNumber > currentStep
                val lineColor = if (stepNumber <= currentStep) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                }

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.5.dp)
                        .drawBehind {
                            if (isUpcomingLine) {
                                drawLine(
                                    color = lineColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                                    strokeWidth = size.height,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                )
                            } else {
                                drawLine(
                                    color = lineColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                                    strokeWidth = size.height
                                )
                            }
                        }
                )
            }

            // Step circle + Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                val circleModifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)

                Box(
                    modifier = when {
                        isDone -> circleModifier.background(MaterialTheme.colorScheme.primary)
                        isCurrent -> circleModifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else -> circleModifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.68f),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.64f)
                    },
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

internal fun Modifier.tabletContentWidth(): Modifier = this
    .fillMaxWidth()
    .widthIn(max = TabletContentMaxWidth)
