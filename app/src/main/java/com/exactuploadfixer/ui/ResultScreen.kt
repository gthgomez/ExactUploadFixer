package com.exactuploadfixer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.exactuploadfixer.R
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.exactuploadfixer.domain.FixFailure
import com.exactuploadfixer.export.ExportManager
import com.workspace.design.AppPrimaryButton
import com.workspace.design.AppNeutralButton
import com.workspace.design.GlassCard
import com.workspace.design.GlassSurfaceStyle
import com.workspace.design.GlassTint

/** Secondary checklist/meta tier — validation copy steps back from data panel. */
private fun ColorScheme.secondaryHelperText() = primary.copy(alpha = 0.54f)

private const val CleanCardAlpha = 1.0f

private fun ColorScheme.cleanCard(alpha: Float = CleanCardAlpha) = surface.copy(alpha = alpha)
private fun ColorScheme.cleanBorder(alpha: Float = 0.38f) = outlineVariant.copy(alpha = alpha)

/**
 * Screen 3: Show result — success or failure.
 *
 * Hierarchy decisions:
 * - Success headline + subtitle: immediate emotional resolution
 * - Image compare: shows the transformation (trust via transparency)
 * - Before/after stats: data layer — proves the constraint was met
 * - Requirements checklist: explicit pass confirmation
 * - Fallback note (when needed): honest max-compression context before Save
 * - Actions: dominant, clear, primary = Save
 *
 * Failure state: not just an error — provides a clear repair path.
 */
@Composable
fun ResultScreen(
    ui: AppUiState,
    onBackToEdit: () -> Unit,
    onStartOver: () -> Unit
) {
    val context = LocalContext.current
    val processedImage = ui.result
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showOriginal by remember { mutableStateOf(false) }

    // Resolve strings at composition time (resource reads stay inside Compose so
    // configuration changes invalidate correctly) — lint: LocalContextGetResourceValueCall.
    val saveSuccessMessage = processedImage?.let {
        stringResource(R.string.result_save_success, it.fileSizeBytes / 1024)
    }
    val saveFailedMessage = stringResource(R.string.result_save_failed)
    val snackbarOpenLabel = stringResource(R.string.result_snackbar_open)

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg"),
        onResult = { uri ->
            if (uri != null && processedImage != null) {
                val tmp = ExportManager.createTempFile(context, processedImage.bytes)
                val ok = ExportManager.saveToUri(context, tmp, uri)
                ExportManager.cleanUpCache(context)
                scope.launch {
                    if (ok) {
                        snackbarHostState.showSnackbar(
                            saveSuccessMessage ?: "",
                            actionLabel = snackbarOpenLabel
                        )
                    } else {
                        snackbarHostState.showSnackbar(saveFailedMessage)
                    }
                }
            }
        }
    )

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .tabletContentWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        StepIndicator(currentStep = 3)

        Spacer(Modifier.height(10.dp))

        if (processedImage != null) {

            // ── Success block — dominant element ─────────────────────────
            val subtitle = buildResultSubtitle(
                sourceSizeBytes = ui.sourceSizeBytes,
                outputSizeBytes = processedImage.fileSizeBytes,
                requestedKb = if (ui.selectedPreset == null) ui.maxSizeKbInput.toLongOrNull() else null,
                presetLabel = ui.selectedPreset?.label
            )
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceStyle = GlassSurfaceStyle.Standard,
                tint = GlassTint.Cyan
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF166534),
                        modifier = Modifier.size(28.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            stringResource(R.string.result_headline_success),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF166534).copy(alpha = 0.80f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Image compare viewer ──────────────────────────────────────
            ImageCompareViewer(
                originalUri = ui.selectedUri,
                processedBytes = processedImage.bytes,
                showOriginal = showOriginal,
                onToggle = { showOriginal = !showOriginal }
            )

            Spacer(Modifier.height(12.dp))

            if (processedImage.qualityUsed < 50) {
                AssistChip(
                    onClick = onBackToEdit,
                    label = { Text(stringResource(R.string.result_heavy_compression_tip)) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.TipsAndUpdates,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Before / after stats ──────────────────────────────────────
            BeforeAfterSummary(
                sourceSizeBytes = ui.sourceSizeBytes,
                outputSizeBytes = processedImage.fileSizeBytes,
                outputWidth = processedImage.width,
                outputHeight = processedImage.height,
                outputQuality = processedImage.qualityUsed
            )

            Spacer(Modifier.height(10.dp))

            // ── Requirements checklist ────────────────────────────────────
            RequirementsSummary(ui = ui)

            // Fallback only: extra reassurance when output hit max compression
            if (ui.isFallback) {
                Spacer(Modifier.height(12.dp))
                ClosingReassuranceFallbackNote()
            }

            Spacer(Modifier.height(18.dp))

            // ── Actions ───────────────────────────────────────────────────
            ResultActionPanel(
                onSave = {
                    // Suggested filename matches the "Saved as fixed_%dkb.jpg" snackbar copy.
                    val suggestedName = processedImage.let { "fixed_${it.fileSizeBytes / 1024}kb.jpg" }
                    saveLauncher.launch(suggestedName)
                },
                onShare = {
                    val tmp = ExportManager.createTempFile(context, processedImage.bytes)
                    context.startActivity(
                        android.content.Intent.createChooser(
                            ExportManager.getShareIntent(context, tmp),
                            "Share"
                        )
                    )
                },
                onBackToEdit = onBackToEdit,
                onStartOver = onStartOver
            )

        } else {
            // ── Failure state — always provides a repair path ─────────────
            FailureState(
                failure = ui.resultFailure,
                onBackToEdit = onBackToEdit
            )
        }

        Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ResultActionPanel(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onBackToEdit: () -> Unit,
    onStartOver: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceStyle = GlassSurfaceStyle.Standard,
        tint = GlassTint.Neutral
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.result_actions_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.result_actions_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondaryHelperText()
            )

            // Primary: Save — filled, full-width, dominant
            AppPrimaryButton(
                text = stringResource(R.string.result_action_save),
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            // Secondary: Share — outlined
            AppNeutralButton(
                text = stringResource(R.string.result_action_share),
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            // Tertiary: text actions
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = onBackToEdit,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.result_action_edit_again))
                }
                TextButton(
                    onClick = onStartOver,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.result_action_start_over))
                }
            }
        }
    }
}

// ── Dynamic subtitle — confirms the actual outcome, not a generic phrase ──────

private fun buildResultSubtitle(
    sourceSizeBytes: Long?,
    outputSizeBytes: Long,
    requestedKb: Long?,
    presetLabel: String?
): String {
    val outKb = outputSizeBytes / 1024
    return when {
        presetLabel != null ->
            "Matches $presetLabel — $outKb KB."
        requestedKb != null && sourceSizeBytes != null && sourceSizeBytes > 0 -> {
            val srcKb = sourceSizeBytes / 1024
            "$srcKb KB → $outKb KB — under $requestedKb KB."
        }
        requestedKb != null ->
            "$outKb KB — under your $requestedKb KB limit."
        else ->
            "Output $outKb KB."
    }
}

// ── Closing reassurance ───────────────────────────────────────────────────────
//
// A single calm statement placed between the checklist and the action buttons.
// Not a heading, not a card. Low visual weight, high psychological value.

@Composable
private fun ClosingReassuranceFallbackNote() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.TipsAndUpdates,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.result_fallback_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondaryHelperText(),
            textAlign = TextAlign.Center
        )
    }
}

// ── Failure state — structured error with repair guidance ─────────────────────
//
// Design intent: a user hitting this screen is already frustrated.
// The UI should not pile on. Failure state = error + specific remedy.

@Composable
private fun FailureState(
    failure: FixFailure?,
    onBackToEdit: () -> Unit
) {
    val title = when (failure) {
        FixFailure.CompressionCouldNotMeetMaxSize -> stringResource(R.string.failure_title_compression)
        FixFailure.UnsupportedMimeType            -> stringResource(R.string.failure_title_mime)
        FixFailure.ProcessingTimedOut             -> stringResource(R.string.failure_title_timeout)
        FixFailure.DecodeFailed                   -> stringResource(R.string.failure_title_decode)
        FixFailure.MemoryBudgetExceeded           -> stringResource(R.string.failure_title_memory)
        FixFailure.InvalidTargetDimensions        -> stringResource(R.string.failure_title_dimensions)
        FixFailure.InvalidInput                   -> stringResource(R.string.failure_title_input)
        FixFailure.SaveFailed                     -> stringResource(R.string.failure_title_save)
        null                                      -> stringResource(R.string.failure_title_unknown)
    }
    val detail = when (failure) {
        FixFailure.CompressionCouldNotMeetMaxSize -> stringResource(R.string.failure_detail_compression)
        FixFailure.UnsupportedMimeType            -> stringResource(R.string.failure_detail_mime)
        FixFailure.ProcessingTimedOut             -> stringResource(R.string.failure_detail_timeout)
        FixFailure.DecodeFailed                   -> stringResource(R.string.failure_detail_decode)
        FixFailure.MemoryBudgetExceeded           -> stringResource(R.string.failure_detail_memory)
        FixFailure.InvalidTargetDimensions        -> stringResource(R.string.failure_detail_dimensions)
        FixFailure.InvalidInput                   -> stringResource(R.string.failure_detail_input)
        FixFailure.SaveFailed                     -> stringResource(R.string.failure_detail_save)
        null                                      -> stringResource(R.string.failure_detail_unknown)
    }
    val remedy: String? = when (failure) {
        FixFailure.CompressionCouldNotMeetMaxSize -> stringResource(R.string.failure_remedy_compression)
        FixFailure.UnsupportedMimeType            -> stringResource(R.string.failure_remedy_mime)
        FixFailure.ProcessingTimedOut             -> stringResource(R.string.failure_remedy_timeout)
        FixFailure.DecodeFailed                   -> stringResource(R.string.failure_remedy_decode)
        FixFailure.MemoryBudgetExceeded           -> stringResource(R.string.failure_remedy_memory)
        FixFailure.InvalidTargetDimensions        -> stringResource(R.string.failure_remedy_dimensions)
        FixFailure.SaveFailed                     -> stringResource(R.string.failure_remedy_save)
        FixFailure.InvalidInput, null             -> null
    }

    Spacer(Modifier.height(40.dp))

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceStyle = GlassSurfaceStyle.Standard,
        tint = GlassTint.Error
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            if (remedy != null) {
                Spacer(Modifier.height(4.dp))
                GlassCard(
                    modifier = Modifier.wrapContentSize(),
                    surfaceStyle = GlassSurfaceStyle.Quiet,
                    tint = GlassTint.Neutral
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            remedy,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            AppPrimaryButton(
                text = stringResource(R.string.failure_action_back),
                onClick = onBackToEdit,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    }
}

// ── Image compare viewer ──────────────────────────────────────────────────────

@Composable
private fun ImageCompareViewer(
    originalUri: android.net.Uri?,
    processedBytes: ByteArray,
    showOriginal: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompareTab(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.compare_tab_fixed),
                selected = !showOriginal,
                onClick = { if (showOriginal) onToggle() }
            )
            CompareTab(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.compare_tab_original),
                selected = showOriginal,
                onClick = { if (!showOriginal) onToggle() }
            )
        }

        AsyncImage(
            model = if (showOriginal) originalUri else processedBytes,
            contentDescription = if (showOriginal) stringResource(R.string.compare_tab_original) else stringResource(R.string.compare_tab_fixed),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 290.dp),
            contentScale = ContentScale.Fit
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (showOriginal) stringResource(R.string.compare_label_original) else stringResource(R.string.compare_label_fixed),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (showOriginal) MaterialTheme.colorScheme.secondaryHelperText()
                else MaterialTheme.colorScheme.primary
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.cleanBorder(0.22f)),
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.compare_toggle_cd),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (showOriginal) stringResource(R.string.compare_toggle_see_fixed) else stringResource(R.string.compare_toggle_see_original),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareTab(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Tab },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        else MaterialTheme.colorScheme.cleanCard(0.84f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
            else MaterialTheme.colorScheme.cleanBorder(0.18f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Before / after stats ──────────────────────────────────────────────────────

@Composable
private fun BeforeAfterSummary(
    sourceSizeBytes: Long?,
    outputSizeBytes: Long,
    outputWidth: Int,
    outputHeight: Int,
    outputQuality: Int
) {
    val reduced = sourceSizeBytes != null && sourceSizeBytes > outputSizeBytes
    val reductionPercent = if (sourceSizeBytes != null && sourceSizeBytes > 0) {
        ((sourceSizeBytes - outputSizeBytes) * 100 / sourceSizeBytes).toInt()
    } else {
        null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.cleanCard(0.74f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.cleanBorder(0.16f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.result_output_summary_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (reductionPercent != null && reduced) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.result_reduction_badge, reductionPercent),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.result_stat_before_size),
                    value = if (sourceSizeBytes != null && sourceSizeBytes > 0L) "${sourceSizeBytes / 1024} KB" else "—",
                    supporting = stringResource(R.string.result_stat_before_supporting),
                    emphasize = false
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.result_stat_after_size),
                    value = "${outputSizeBytes / 1024} KB",
                    supporting = if (reduced) stringResource(R.string.result_stat_after_reduced) else stringResource(R.string.result_stat_after_no_reduction),
                    emphasize = true,
                    icon = if (reduced) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.result_stat_dimensions),
                    value = "$outputWidth × $outputHeight",
                    supporting = stringResource(R.string.result_stat_dimensions_supporting)
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.result_stat_compression),
                    value = when {
                        outputQuality < 50  -> stringResource(R.string.result_compression_high)
                        outputQuality <= 80 -> stringResource(R.string.result_compression_balanced)
                        else                -> stringResource(R.string.result_compression_light)
                    },
                    supporting = stringResource(R.string.result_compression_supporting, outputQuality)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    supporting: String,
    emphasize: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    GlassCard(
        modifier = modifier,
        surfaceStyle = if (emphasize) GlassSurfaceStyle.Standard else GlassSurfaceStyle.Quiet,
        tint = if (emphasize) GlassTint.Cyan else GlassTint.Neutral
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (emphasize) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (emphasize) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    value,
                    style = if (emphasize) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (emphasize) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = if (emphasize) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                else MaterialTheme.colorScheme.secondaryHelperText()
            )
        }
    }
}

// ── Requirements checklist ────────────────────────────────────────────────────

@Composable
private fun RequirementsSummary(ui: AppUiState) {
    val preset        = ui.selectedPreset
    val requestedKb   = if (preset == null) ui.maxSizeKbInput.toLongOrNull() else null
    val requestedW    = if (preset == null) ui.widthInput.toIntOrNull() else null
    val requestedH    = if (preset == null) ui.heightInput.toIntOrNull() else null

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceStyle = GlassSurfaceStyle.Standard,
        tint = GlassTint.Cyan
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.result_requirements_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (preset != null) {
                CheckRow("${preset.label} — ${preset.width} × ${preset.height} px, max ${preset.maxBytes / 1024} KB")
            } else {
                if (requestedKb != null)   CheckRow(stringResource(R.string.result_requirement_size, requestedKb))
                if (requestedW != null && requestedH != null) {
                    CheckRow(stringResource(R.string.result_requirement_dimensions, requestedW, requestedH))
                }
            }
            CheckRow(stringResource(R.string.result_requirement_ready))
        }
    }
}

@Composable
private fun CheckRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
        )
    }
}
