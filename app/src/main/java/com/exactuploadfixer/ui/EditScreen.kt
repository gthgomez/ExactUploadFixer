package com.exactuploadfixer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.exactuploadfixer.R
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.exactuploadfixer.domain.PRESETS
import com.exactuploadfixer.domain.Preset
import kotlin.math.roundToInt
import com.workspace.design.AppPrimaryButton
import com.workspace.design.AppNeutralButton
import com.workspace.design.GlassCard
import com.workspace.design.GlassSurfaceStyle
import com.workspace.design.GlassTint
import com.workspace.design.ConfirmDeleteDialog
import android.app.Activity
import androidx.compose.ui.platform.LocalContext


/** Secondary helper/meta tier — a softer sky-blue for supporting copy. */
private fun ColorScheme.secondaryHelperText() = primary.copy(alpha = 0.54f)

/**
 * Screen 2: Set constraints and trigger processing.
 *
 * Visual hierarchy decisions:
 * - Inline photo preview: grounding, shows user what they're working with
 * - Size input + chips: primary action zone — most users only need this
 * - Confidence note: demoted to a left-bordered inline hint, not a card
 * - Preset section: outcome-framed ("skip the research"), not feature-framed
 * - CTA: always dominant — nothing above it should match its visual weight
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    ui: AppUiState,
    onMaxSizeChanged: (String) -> Unit,
    onWidthChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onPresetSelected: (Preset) -> Unit,
    onEntitlementRefreshRequested: () -> Unit,
    onProcessClick: () -> Unit,
    onBuyProClick: (Activity) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showAdvanced by rememberSaveable { mutableStateOf(ui.widthInput.isNotEmpty() || ui.heightInput.isNotEmpty()) }
    var showProSheet by remember { mutableStateOf(false) }
    var showStorePaywall by remember { mutableStateOf(false) }
    var showCustomerCenter by remember { mutableStateOf(false) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    var showConfirmStartOver by remember { mutableStateOf(false) }
    var selectedPresetForSheet by remember { mutableStateOf<Preset?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val previewSheetState = rememberModalBottomSheetState()
    val glassSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val glassSheetColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)

    LaunchedEffect(ui.currentQuality) {
        val q = ui.currentQuality ?: return@LaunchedEffect
        if (q % 5 == 0) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
    }

    LaunchedEffect(ui.isProcessing) {
        if (!ui.isProcessing && ui.selectedUri != null && (ui.result != null || ui.resultFailure != null)) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .tabletContentWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp) // manual spacing for tighter control
        ) {
            StepIndicator(currentStep = 2)

        Spacer(Modifier.height(8.dp))

        // ── Inline photo preview ──────────────────────────────────────────
        InlinePhotoPreview(
            uri = ui.selectedUri,
            sourceSizeBytes = ui.sourceSizeBytes,
            onExpandClick = { showPreviewSheet = true }
        )

        Spacer(Modifier.height(20.dp))

        // ── Step label: frames the input as a directed task, not a form ──
        StepLabel(label = stringResource(R.string.edit_section_custom_size))

        Spacer(Modifier.height(12.dp))

        // ── Size input + quick chips ──────────────────────────────────────
        OutlinedTextField(
            value = ui.maxSizeKbInput,
            onValueChange = onMaxSizeChanged,
            label = { Text(stringResource(R.string.edit_size_label)) },
            placeholder = { Text(stringResource(R.string.edit_size_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (ui.editError != null) liveRegion = LiveRegionMode.Polite
                },
            singleLine = true,
            enabled = !ui.isProcessing,
            shape = RoundedCornerShape(16.dp),
            isError = ui.editError != null,
            suffix = { Text(stringResource(R.string.edit_size_suffix)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                disabledTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.84f),
                disabledLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
                errorLabelColor = MaterialTheme.colorScheme.error,
                focusedPlaceholderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                errorPlaceholderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                focusedSuffixColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                unfocusedSuffixColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
                disabledSuffixColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                errorSuffixColor = MaterialTheme.colorScheme.error,
                focusedSupportingTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f),
                unfocusedSupportingTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f),
                disabledSupportingTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                errorSupportingTextColor = MaterialTheme.colorScheme.error
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            supportingText = {
                if (ui.editError != null) {
                    Text(
                        text = ui.editError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        stringResource(R.string.edit_size_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondaryHelperText()
                    )
                }
            }
        )

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChipItem(stringResource(R.string.edit_chip_200kb)) { onMaxSizeChanged("200") }
            SuggestionChipItem(stringResource(R.string.edit_chip_240kb)) { onMaxSizeChanged("240") }
            SuggestionChipItem(stringResource(R.string.edit_chip_500kb)) { onMaxSizeChanged("500") }
        }

        Spacer(Modifier.height(8.dp))

        ProcessingNote(ui = ui)

        Spacer(Modifier.height(12.dp))

        // ── Dimensions toggle ─────────────────────────────────────────────
        TextButton(
            onClick = { showAdvanced = !showAdvanced },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .align(Alignment.Start),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        ) {
            Icon(
                if (showAdvanced) Icons.Default.Tune else Icons.Default.Add,
                contentDescription = stringResource(R.string.edit_dimensions_show),
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (showAdvanced) stringResource(R.string.edit_dimensions_hide)
                else stringResource(R.string.edit_dimensions_show),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = showAdvanced) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                surfaceStyle = GlassSurfaceStyle.Quiet,
                tint = GlassTint.Neutral
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = ui.widthInput,
                            onValueChange = onWidthChanged,
                            label = { Text(stringResource(R.string.edit_width_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !ui.isProcessing,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                disabledTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
                                disabledLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
                                errorLabelColor = MaterialTheme.colorScheme.error
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                        OutlinedTextField(
                            value = ui.heightInput,
                            onValueChange = onHeightChanged,
                            label = { Text(stringResource(R.string.edit_height_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !ui.isProcessing,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                disabledTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.76f),
                                disabledLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
                                errorLabelColor = MaterialTheme.colorScheme.error
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                        )
                    }
                    Text(
                        stringResource(R.string.edit_dimensions_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondaryHelperText()
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        StepLabel(label = stringResource(R.string.edit_section_presets))

        Spacer(Modifier.height(12.dp))

        PresetSection(
            ui = ui,
            onPresetSelected = onPresetSelected,
            onLockedClick = { preset ->
                selectedPresetForSheet = preset
                showProSheet = true
            }
        )

        Spacer(Modifier.height(20.dp))

        // ── Progress ──────────────────────────────────────────────────────
        if (ui.isProcessing) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val progress = ui.processingProgress
                val label = if (progress != null) {
                    stringResource(R.string.edit_processing_quality, (progress * 100).roundToInt())
                } else {
                    stringResource(R.string.edit_processing_label)
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .semantics {
                                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                            }
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showConfirmStartOver) {
            ConfirmDeleteDialog(
                title = stringResource(R.string.discard_dialog_title),
                message = stringResource(R.string.discard_dialog_message),
                confirmLabel = stringResource(R.string.discard_dialog_confirm),
                onConfirm = {
                    showConfirmStartOver = false
                    onBack()
                },
                onDismiss = { showConfirmStartOver = false }
            )
        }

        // ── Primary CTA — always dominant ────────────────────────────────
        AppPrimaryButton(
            text = if (ui.isProcessing) stringResource(R.string.edit_cta_fixing) else stringResource(R.string.edit_cta_fix),
            onClick = onProcessClick,
            enabled = !ui.isProcessing && ui.maxSizeKbInput.toLongOrNull()?.let { it > 0 } == true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(Modifier.height(4.dp))

        TextButton(
            onClick = { showConfirmStartOver = true },
            enabled = !ui.isProcessing,
            modifier = Modifier
                .align(Alignment.Start)
                .heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.edit_cancel), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.68f))
        }

            Spacer(Modifier.height(8.dp))
        }
    }

    // ── Photo preview sheet ───────────────────────────────────────────────────
    if (showPreviewSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPreviewSheet = false },
            sheetState = previewSheetState,
            shape = glassSheetShape,
            containerColor = glassSheetColor,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.preview_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    model = ui.selectedUri,
                    contentDescription = "Photo preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(24.dp))
                FilledTonalButton(
                    onClick = { showPreviewSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.preview_sheet_close)) }
            }
        }
    }

    // ── Pro paywall sheet ─────────────────────────────────────────────────────
    if (showProSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProSheet = false },
            sheetState = sheetState,
            shape = glassSheetShape,
            containerColor = glassSheetColor,
            tonalElevation = 0.dp
        ) {
            val preset = selectedPresetForSheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Headline: names the user's actual saved effort
                Text(
                    stringResource(R.string.paywall_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    stringResource(R.string.paywall_guarantee),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondaryHelperText(),
                    textAlign = TextAlign.Center
                )

                Text(
                    stringResource(R.string.paywall_body),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondaryHelperText()
                )

                // Show the specific preset the user tapped, with its practical note
                if (preset != null) {
                    ProPaywallPresetCard(preset = preset)
                }

                Spacer(Modifier.height(2.dp))

                AppPrimaryButton(
                    text = stringResource(R.string.paywall_cta),
                    onClick = {
                        showProSheet = false
                        showStorePaywall = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                TextButton(onClick = {
                    showProSheet = false
                    showCustomerCenter = true
                }) {
                    Text(
                        stringResource(R.string.paywall_manage),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                TextButton(onClick = { showProSheet = false }) {
                    Text(
                        stringResource(R.string.paywall_dismiss),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    StoreUpgradeHost(
        showPaywall = showStorePaywall,
        showCustomerCenter = showCustomerCenter,
        onDismissPaywall = { showStorePaywall = false },
        onDismissCustomerCenter = { showCustomerCenter = false },
        onEntitlementChanged = onEntitlementRefreshRequested,
        onRequestPurchase = {
            (context as? Activity)?.let { activity ->
                onBuyProClick(activity)
            }
        }
    )
}

// ── Inline photo preview ──────────────────────────────────────────────────────

@Composable
private fun InlinePhotoPreview(
    uri: android.net.Uri?,
    sourceSizeBytes: Long?,
    onExpandClick: () -> Unit
) {
    if (uri == null) return

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceStyle = GlassSurfaceStyle.Standard,
        tint = GlassTint.Neutral
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Selected photo",
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    stringResource(R.string.preview_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                val meta = if (sourceSizeBytes != null && sourceSizeBytes > 0L) {
                    val sizeLabel = if (sourceSizeBytes >= 1_048_576)
                        "%.1f MB".format(sourceSizeBytes / 1_048_576f)
                    else
                        "${sourceSizeBytes / 1024} KB"
                    "$sizeLabel · JPEG"
                } else "JPEG"
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondaryHelperText()
                )
            }

            IconButton(onClick = onExpandClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = stringResource(R.string.preview_expand_cd),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StepLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ProcessingNote(ui: AppUiState) {
    val maxKb = ui.maxSizeKbInput.toLongOrNull()
    val hasSize = maxKb != null && maxKb > 0
    val hasWidth = ui.widthInput.toIntOrNull()?.let { it > 0 } == true
    val hasHeight = ui.heightInput.toIntOrNull()?.let { it > 0 } == true
    val hasDimensions = hasWidth && hasHeight
    val hasPreset = ui.selectedPreset != null

    if (!hasSize && !hasPreset) {
        Text(
            stringResource(R.string.edit_note_placeholder),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondaryHelperText()
        )
        return
    }

    val accentSurface = Color(0xFFEEF2FF)
    val accentBorder  = Color(0xFF2563EB).copy(alpha = 0.22f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceStyle = GlassSurfaceStyle.Quiet,
        tint = GlassTint.Cyan
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier
                    .size(16.dp)
                    .offset(y = 1.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasPreset) {
                    val p = requireNotNull(ui.selectedPreset)
                    NoteItem("Will apply ${p.label}: ${p.width} × ${p.height} px, under ${p.maxBytes / 1024} KB", primary = true)
                } else {
                    if (hasSize) {
                        val srcPart = ui.sourceSizeBytes?.let { if (it > 0L) "${it / 1024} KB → " else "" } ?: ""
                        NoteItem("Will compress ${srcPart}under $maxKb KB", primary = true)
                    }
                    if (hasDimensions) NoteItem("Will scale to ${ui.widthInput} × ${ui.heightInput} px", primary = false)
                }
                NoteItem(stringResource(R.string.edit_note_jpeg_preserved), primary = false)
                NoteItem(stringResource(R.string.edit_note_quality_preserved), primary = false)
            }
        }
    }
}

@Composable
private fun NoteItem(text: String, primary: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
        color = if (primary) Color(0xFF1D4ED8) else Color(0xFF374151)
    )
}

// ── Pro preset section ────────────────────────────────────────────────────────

@Composable
private fun PresetSection(
    ui: AppUiState,
    onPresetSelected: (Preset) -> Unit,
    onLockedClick: (Preset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(PRESETS) { preset ->
                PresetChip(
                    preset = preset,
                    isSelected = ui.selectedPreset?.id == preset.id,
                    isUnlocked = ui.isProUnlocked,
                    onClick = {
                        if (ui.isProUnlocked) onPresetSelected(preset)
                        else onLockedClick(preset)
                    }
                )
            }
        }

        // Context line below chips — only shown when locked
        if (!ui.isProUnlocked) {
            Text(
                text = stringResource(R.string.edit_presets_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondaryHelperText()
            )
        }
    }
}

// ── Paywall preset detail card ────────────────────────────────────────────────

@Composable
private fun ProPaywallPresetCard(preset: Preset) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        surfaceStyle = GlassSurfaceStyle.Standard,
        tint = GlassTint.Cyan
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                preset.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${preset.width} × ${preset.height} px  ·  max ${preset.maxBytes / 1024} KB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondaryHelperText()
            )
            if (preset.note.isNotBlank()) {
                Text(
                    preset.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondaryHelperText(),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            Text(
                "Verified ${preset.lastVerified}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondaryHelperText().copy(alpha = 0.72f)
            )
        }
    }
}

// ── Chips ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetChip(
    preset: Preset,
    isSelected: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = Modifier.heightIn(min = 42.dp),
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                preset.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected || !isUnlocked) FontWeight.SemiBold else FontWeight.Medium,
                color = when {
                    isSelected  -> MaterialTheme.colorScheme.primary
                    !isUnlocked -> MaterialTheme.colorScheme.onSecondaryContainer
                    else        -> MaterialTheme.colorScheme.secondaryHelperText()
                }
            )
        },
        shape = RoundedCornerShape(16.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (!isUnlocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.60f)
                          else MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp
        ),
        leadingIcon = {
            Icon(
                imageVector = when {
                    !isUnlocked -> Icons.Default.Lock
                    isSelected  -> Icons.Default.Check
                    else        -> Icons.AutoMirrored.Outlined.Rule
                },
                contentDescription = if (!isUnlocked) "Locked preset" else null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
                tint = when {
                    isSelected  -> MaterialTheme.colorScheme.primary
                    !isUnlocked -> MaterialTheme.colorScheme.onSecondaryContainer
                    else        -> MaterialTheme.colorScheme.secondaryHelperText()
                }
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (!isUnlocked) MaterialTheme.colorScheme.secondaryContainer
                             else MaterialTheme.colorScheme.surfaceContainerLow,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionChipItem(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        shape = RoundedCornerShape(16.dp)
    )
}
