package com.exactuploadfixer.fakes

import android.net.Uri
import com.exactuploadfixer.domain.FixConstraints
import com.exactuploadfixer.domain.FixFailure
import com.exactuploadfixer.domain.FixResult
import com.exactuploadfixer.domain.ProcessedImage
import com.exactuploadfixer.domain.UploadFixerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Test double for [UploadFixerEngine]. Configurable emissions, tracks call count.
 * Lives in src/test/ only — never shipped in production APK.
 */
class FakeUploadFixerEngine : UploadFixerEngine {

    /** Default to a plausible success result. Override per test. */
    var emissions: List<FixResult> = listOf(
        FixResult.Success(
            ProcessedImage(
                bytes = ByteArray(1024),
                width = 400,
                height = 400,
                qualityUsed = 80,
                fileSizeBytes = 1024L
            )
        )
    )

    var callCount = 0
    var lastConstraints: FixConstraints? = null

    override suspend fun process(sourceUri: Uri, constraints: FixConstraints): Flow<FixResult> = flow {
        callCount++
        lastConstraints = constraints
        emissions.forEach { emit(it) }
    }
}

/** Convenience builder for a failure result. */
fun fakeFailure(reason: FixFailure = FixFailure.InvalidInput): FixResult =
    FixResult.Failure(reason)
