package com.exactuploadfixer.domain

import android.net.Uri

/**
 * Core processing contract. UI and ViewModel depend only on this interface.
 * ImageProcessor implements it. Billing knows nothing about it.
 *
 * Source: ChatGPT Step 2 "Minimal engine boundary".
 */
import kotlinx.coroutines.flow.Flow

interface UploadFixerEngine {
    suspend fun process(
        sourceUri: Uri,
        constraints: FixConstraints
    ): Flow<FixResult>
}
