package com.example.moneymate.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptOcrProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(imageUri: Uri): String {
        val image = InputImage.fromFilePath(context, imageUri)
        val fullImageText = recognizer.process(image).await().text
        val tiledText = recognizeLongReceiptTiles(imageUri)
        return mergeOcrText(fullImageText, tiledText)
    }

    private suspend fun recognizeLongReceiptTiles(imageUri: Uri): String {
        val bitmap = loadBitmapFromUri(imageUri) ?: return ""
        if (!bitmap.shouldUseTiledOcr()) return ""

        val tileHeight = (bitmap.width * TILE_HEIGHT_WIDTH_RATIO)
            .toInt()
            .coerceIn(MIN_TILE_HEIGHT, MAX_TILE_HEIGHT)
        val overlap = (tileHeight * TILE_OVERLAP_RATIO).toInt()
        val step = (tileHeight - overlap).coerceAtLeast(MIN_TILE_STEP)

        val tileTexts = mutableListOf<String>()
        var top = 0
        while (top < bitmap.height) {
            val actualTileHeight = minOf(tileHeight, bitmap.height - top)
            val tile = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, actualTileHeight)
            try {
                val tileImage = InputImage.fromBitmap(tile, 0)
                val text = recognizer.process(tileImage).await().text
                if (text.isNotBlank()) tileTexts.add(text)
            } finally {
                tile.recycle()
            }

            if (top + actualTileHeight >= bitmap.height) break
            top += step
        }

        return tileTexts.joinToString("\n")
    }

    private fun loadBitmapFromUri(imageUri: Uri): Bitmap? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        }.getOrNull()
    }

    private fun Bitmap.shouldUseTiledOcr(): Boolean {
        val aspectRatio = height.toFloat() / width.coerceAtLeast(1)
        return aspectRatio >= LONG_IMAGE_ASPECT_RATIO || height >= LONG_IMAGE_MIN_HEIGHT
    }

    private fun mergeOcrText(vararg texts: String): String {
        return texts
            .flatMap { text ->
                text.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
            .distinctBy { it.lowercase() }
            .joinToString("\n")
    }

    private companion object {
        const val LONG_IMAGE_ASPECT_RATIO = 2.2f
        const val LONG_IMAGE_MIN_HEIGHT = 2_400
        const val TILE_HEIGHT_WIDTH_RATIO = 1.6f
        const val TILE_OVERLAP_RATIO = 0.18f
        const val MIN_TILE_HEIGHT = 900
        const val MAX_TILE_HEIGHT = 1_800
        const val MIN_TILE_STEP = 650
    }
}
