package com.example.smartwardrobe.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.model.WardrobeItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Utility class for compositing multiple wardrobe item images into a single outfit image.
 *
 * Creates layered composites with category-based vertical positioning:
 * - SHOES: Bottom 20% of canvas
 * - BOTTOM: 30-85% from top
 * - TOP: 15-60% from top
 * - OUTERWEAR: 10-80% from top (covers most)
 * - ACCESSORIES: Top 30%
 * - DRESS: 15-80% from top (full coverage)
 * - OTHER: Centered (full canvas)
 */
object ImageCompositor {

    private const val TAG = "ImageCompositor"

    // Canvas size for composite image (1024x1024 for good quality + reasonable file size)
    private const val CANVAS_WIDTH = 1024
    private const val CANVAS_HEIGHT = 1024

    // Maximum dimension when loading source images (to prevent OOM)
    private const val MAX_SOURCE_DIMENSION = 2048

    // Composite cache directory name
    private const val COMPOSITE_CACHE_DIR = "outfit_composites"

    // Default max age for cached composites (7 days)
    private const val DEFAULT_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

    // Background removal - color distance threshold (squared Euclidean distance)
    private const val COLOR_DISTANCE_THRESHOLD = 55 * 55

    // Background removal - brightness threshold (0-255)
    private const val BRIGHTNESS_THRESHOLD = 215

    // Corner sample size for background detection (pixels)
    private const val CORNER_SAMPLE_SIZE = 10

    // Minimum foreground coverage - if less than this, background removal was too aggressive
    private const val MIN_FOREGROUND_COVERAGE = 0.20f  // 20% of pixels must remain opaque
    private const val ABSOLUTE_MIN_FOREGROUND = 0.08f  // below this, we really revert

    // Alpha feathering radius (pixels)
    private const val FEATHER_RADIUS = 10

    // Morphological closing radius for merging silhouettes (pixels)
    private const val MORPH_RADIUS = 20

    /**
     * Layering order from bottom to top.
     * Items are drawn in this order so later items appear on top.
     */
    private val LAYER_ORDER = listOf(
        ClothingCategory.SHOES,       // Foundation
        ClothingCategory.BOTTOM,      // Pants/skirts
        ClothingCategory.DRESS,       // Dress over bottoms
        ClothingCategory.TOP,         // Shirts/sweaters
        ClothingCategory.OUTERWEAR,   // Jackets/coats
        ClothingCategory.ACCESSORIES, // Visible details on top
        ClothingCategory.OTHER        // User-defined / misc
    )

    /**
     * Composites multiple wardrobe item images into a single outfit image.
     *
     * @param context Application context for file operations
     * @param selectedItems Map of category to wardrobe item (from OutfitBuilder)
     * @return Uri to the composite PNG file in cache directory
     * @throws IOException if compositing fails or no valid items provided
     */
    fun compositeOutfitImages(
        context: Context,
        selectedItems: Map<ClothingCategory, WardrobeItem>
    ): Uri {
        Log.d(TAG, "Starting outfit composite with ${selectedItems.size} items")

        // Get list of items (we expect this map to already be filtered to non-null values)
        val items = selectedItems.values.toList()

        if (items.isEmpty()) {
            throw IOException("No items selected for compositing")
        }

        // Create composite bitmap with transparent background
        val composite = Bitmap.createBitmap(
            CANVAS_WIDTH,
            CANVAS_HEIGHT,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(composite)
        // Ensure transparent background
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }

        var mergedComposite: Bitmap? = null
        var croppedComposite: Bitmap? = null

        try {
            // Sort items by layering order
            val sortedItems = items.sortedBy { item ->
                LAYER_ORDER.indexOf(item.category).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }

            Log.d(TAG, "Drawing ${sortedItems.size} items in order: ${sortedItems.map { it.category }}")

            var successfullyDrawn = 0

            // Draw each item in order
            for (item in sortedItems) {
                try {
                    // Load bitmap with downsampling
                    val rawBitmap = loadBitmapFromPath(item.imageUrl, MAX_SOURCE_DIMENSION)

                    // Step 1: Remove white background BEFORE positioning
                    // Note: removeWhiteBackground may return the same instance if removal was too aggressive
                    val noBgBitmap = removeWhiteBackground(rawBitmap)

                    // Step 2: Feather alpha edges to create soft matte (only if background was removed)
                    val processedBitmap = if (noBgBitmap !== rawBitmap) {
                        // Background removed successfully, apply feathering
                        val feathered = featherAlphaEdges(noBgBitmap)
                        noBgBitmap.recycle()  // Free the unfeathered cutout
                        rawBitmap.recycle()   // Free original
                        feathered
                    } else {
                        // Background removal was too aggressive, use original (no feathering needed)
                        rawBitmap
                    }

                    // Step 3: Position and draw the processed bitmap (cutout with soft edges or original)
                    val destRect = calculatePositionedRect(processedBitmap, item.category)
                    canvas.drawBitmap(processedBitmap, null, destRect, paint)
                    processedBitmap.recycle()  // Free processed bitmap

                    successfullyDrawn++
                    Log.d(TAG, "Drew ${item.category} at bounds: $destRect")

                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Failed to load/draw item ${item.name} (${item.category}): ${e.message}"
                    )
                    // Continue with other items
                }
            }

            if (successfullyDrawn == 0) {
                throw IOException("Failed to load any outfit items")
            }

            Log.d(TAG, "Successfully drew $successfullyDrawn items")

            // Step 4: Merge all items into single connected silhouette (morphological closing)
            // This bridges gaps between shirt/jeans and creates one unified shape
            mergedComposite = mergeIntoConnectedSilhouette(composite)
            composite.recycle()  // Free original composite

            // Step 5: Crop transparent margins to remove empty space
            croppedComposite = cropTransparentMargins(mergedComposite)
            mergedComposite.recycle()  // Free merged composite

            // Step 6: Save final composite to cache directory
            val compositeFile = saveCompositeToCache(croppedComposite, context)

            return Uri.fromFile(compositeFile)

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during compositing", e)
            throw IOException("Insufficient memory for compositing")
        } finally {
            // Clean up bitmaps if not already recycled
            if (!composite.isRecycled) {
                composite.recycle()
            }
            mergedComposite?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
            croppedComposite?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        }
    }

    /**
     * Loads a bitmap from a file path with downsampling to prevent OOM.
     *
     * @param imagePath Absolute path to image file
     * @param maxDimension Maximum width or height (will downsample if larger)
     * @return Loaded bitmap
     * @throws IOException if file doesn't exist or can't be decoded
     */
    private fun loadBitmapFromPath(imagePath: String, maxDimension: Int): Bitmap {
        val file = File(imagePath)

        if (!file.exists()) {
            throw IOException("Image file not found: $imagePath")
        }

        // First decode bounds only to determine sample size
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight

        // Calculate sample size (power of 2 for efficiency)
        var sampleSize = 1
        val maxOriginalDimension = maxOf(imageWidth, imageHeight)

        if (maxOriginalDimension > maxDimension) {
            sampleSize = maxOriginalDimension / maxDimension
            // Round to nearest power of 2
            sampleSize = Integer.highestOneBit(sampleSize).coerceAtLeast(1)
        }

        // Now decode with sample size
        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = BitmapFactory.decodeFile(imagePath, loadOptions)
            ?: throw IOException("Failed to decode image: $imagePath")

        Log.d(
            TAG,
            "Loaded bitmap ${bitmap.width}x${bitmap.height} from $imagePath (sample: $sampleSize)"
        )

        return bitmap
    }

    /**
     * Removes background from a bitmap by making edge-connected background pixels transparent.
     *
     * Uses corner sampling to detect background color, then flood-fills from edges to find
     * all background pixels connected to the border. This preserves white clothing while
     * removing white backgrounds.
     *
     * Algorithm:
     * 1. Sample background color from image corners
     * 2. Mark pixels as "background-like" if they're similar to sampled color AND bright
     * 3. Flood-fill from edges through background-like pixels only
     * 4. Make flood-fill-reachable pixels transparent
     *
     * @param bitmap Source bitmap with background
     * @return New bitmap with edge-connected background removed (transparent)
     */
    private fun removeWhiteBackground(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        Log.d(TAG, "Removing background from ${width}x${height} bitmap...")

        // Step 1: Sample background color from corners
        val (bgR, bgG, bgB) = sampleBackgroundFromCorners(bitmap)
        Log.d(TAG, "Sampled background RGB: ($bgR, $bgG, $bgB)")

        // Step 2: Build candidate grid - which pixels are "background-like"?
        val isCandidate = Array(height) { BooleanArray(width) }
        var candidateCount = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Color distance to background
                val dr = r - bgR
                val dg = g - bgG
                val db = b - bgB
                val distanceSq = dr * dr + dg * dg + db * db

                // Brightness
                val brightness = (r + g + b) / 3

                // Background candidate if: close to bg color AND bright
                if (distanceSq < COLOR_DISTANCE_THRESHOLD && brightness > BRIGHTNESS_THRESHOLD) {
                    isCandidate[y][x] = true
                    candidateCount++
                }
            }
        }

        Log.d(TAG, "Background candidates: $candidateCount pixels")

        // Step 3: Flood-fill from edges through candidates only
        val isBackground = Array(height) { BooleanArray(width) }
        val queue = ArrayDeque<Pair<Int, Int>>()

        // Seed flood-fill from all border pixels that are candidates
        for (x in 0 until width) {
            if (isCandidate[0][x]) {
                queue.add(Pair(x, 0))
                isBackground[0][x] = true
            }
            if (isCandidate[height - 1][x]) {
                queue.add(Pair(x, height - 1))
                isBackground[height - 1][x] = true
            }
        }
        for (y in 0 until height) {
            if (isCandidate[y][0]) {
                queue.add(Pair(0, y))
                isBackground[y][0] = true
            }
            if (isCandidate[y][width - 1]) {
                queue.add(Pair(width - 1, y))
                isBackground[y][width - 1] = true
            }
        }

        // BFS flood-fill
        val directions = arrayOf(
            Pair(-1, 0), Pair(1, 0),  // left, right
            Pair(0, -1), Pair(0, 1)   // up, down
        )

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()

            for ((dx, dy) in directions) {
                val nx = x + dx
                val ny = y + dy

                if (nx in 0 until width && ny in 0 until height &&
                    isCandidate[ny][nx] && !isBackground[ny][nx]
                ) {
                    isBackground[ny][nx] = true
                    queue.add(Pair(nx, ny))
                }
            }
        }

        // Step 4: Build output bitmap
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var transparentPixels = 0
        var opaquePixels = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (isBackground[y][x]) {
                    output.setPixel(x, y, Color.TRANSPARENT)
                    transparentPixels++
                } else {
                    output.setPixel(x, y, bitmap.getPixel(x, y))
                    opaquePixels++
                }
            }
        }

        val totalPixels = width * height
        val transparentPercent = (transparentPixels * 100f / totalPixels)
        val coverage = opaquePixels.toFloat() / totalPixels.toFloat()

        Log.d(TAG, "Background removal: $transparentPercent% pixels made transparent")
        Log.d(TAG, "Foreground coverage after background removal: $coverage")

        // Safety check: only revert if we nuked almost everything
        return if (coverage < ABSOLUTE_MIN_FOREGROUND) {
            Log.w(
                TAG,
                "Background removal WAY too aggressive (coverage=$coverage < $ABSOLUTE_MIN_FOREGROUND). " +
                    "Reverting to original bitmap."
            )
            output.recycle()
            bitmap
        } else {
            if (coverage < MIN_FOREGROUND_COVERAGE) {
                Log.w(
                    TAG,
                    "Low foreground coverage (coverage=$coverage < $MIN_FOREGROUND_COVERAGE) " +
                        "- keeping cutout anyway to avoid white slabs."
                )
            }
            output
        }
    }

    /**
     * Samples background color from the four corners of the image.
     * Takes average of small patches in each corner.
     *
     * @param bitmap Source bitmap
     * @return Triple of (avgR, avgG, avgB)
     */
    private fun sampleBackgroundFromCorners(bitmap: Bitmap): Triple<Int, Int, Int> {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = minOf(CORNER_SAMPLE_SIZE, width / 10, height / 10)

        var totalR = 0
        var totalG = 0
        var totalB = 0
        var sampleCount = 0

        // Define corner regions
        val corners = listOf(
            Pair(0, 0),                           // Top-left
            Pair(width - sampleSize, 0),          // Top-right
            Pair(0, height - sampleSize),         // Bottom-left
            Pair(width - sampleSize, height - sampleSize)  // Bottom-right
        )

        for ((startX, startY) in corners) {
            for (dy in 0 until sampleSize) {
                for (dx in 0 until sampleSize) {
                    val x = startX + dx
                    val y = startY + dy
                    if (x in 0 until width && y in 0 until height) {
                        val pixel = bitmap.getPixel(x, y)
                        totalR += (pixel shr 16) and 0xFF
                        totalG += (pixel shr 8) and 0xFF
                        totalB += pixel and 0xFF
                        sampleCount++
                    }
                }
            }
        }

        val avgR = if (sampleCount > 0) totalR / sampleCount else 255
        val avgG = if (sampleCount > 0) totalG / sampleCount else 255
        val avgB = if (sampleCount > 0) totalB / sampleCount else 255

        return Triple(avgR, avgG, avgB)
    }

    /**
     * Calculates the destination rectangle for drawing an item based on its category.
     * Items are positioned within category-specific vertical bounds and centered horizontally.
     *
     * @param bitmap Source bitmap to position
     * @param category Clothing category determining vertical position
     * @return Destination rectangle on canvas
     */
    private fun calculatePositionedRect(bitmap: Bitmap, category: ClothingCategory): Rect {
        // Get vertical bounds for this category (as fractions of canvas height)
        val (topFraction, bottomFraction) = getCategoryBounds(category)

        val verticalTop = (topFraction * CANVAS_HEIGHT).toInt()
        val verticalBottom = (bottomFraction * CANVAS_HEIGHT).toInt()
        val maxHeight = verticalBottom - verticalTop

        // Calculate scaled dimensions to fit within vertical bounds
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()
        val bitmapAspectRatio = bitmapWidth / bitmapHeight

        var scaledWidth: Int
        var scaledHeight: Int

        // First, constrain by max height
        scaledHeight = maxHeight
        scaledWidth = (maxHeight * bitmapAspectRatio).toInt()

        // If width exceeds canvas, constrain by width instead
        if (scaledWidth > CANVAS_WIDTH) {
            scaledWidth = CANVAS_WIDTH
            scaledHeight = (CANVAS_WIDTH / bitmapAspectRatio).toInt()
            // Ensure we still fit in vertical bounds
            if (scaledHeight > maxHeight) {
                scaledHeight = maxHeight
                scaledWidth = (maxHeight * bitmapAspectRatio).toInt()
            }
        }

        // Center horizontally
        val left = (CANVAS_WIDTH - scaledWidth) / 2
        val right = left + scaledWidth

        // Center vertically within category bounds
        val verticalCenter = (verticalTop + verticalBottom) / 2
        val top = verticalCenter - (scaledHeight / 2)
        val bottom = top + scaledHeight

        // Debug logging to verify positioning
        Log.d(
            TAG,
            "Positioning $category -> bounds=[$topFraction,$bottomFraction], " +
                "rect=[$left,$top,$right,$bottom], bitmap=${bitmap.width}x${bitmap.height}"
        )

        return Rect(left, top, right, bottom)
    }

    /**
     * Returns vertical bounds for each clothing category as fractions of canvas height.
     *
     * Updated with overlapping bounds so TOP and BOTTOM sit close together like real clothing.
     * After compositing, we crop empty margins to remove the "slab" effect.
     *
     * @param category Clothing category
     * @return Pair of (topFraction, bottomFraction) where 0.0 = top, 1.0 = bottom
     */
    private fun getCategoryBounds(category: ClothingCategory): Pair<Float, Float> {
        return when (category) {
            ClothingCategory.SHOES -> Pair(0.85f, 1.0f)       // Bottom 15%
            ClothingCategory.BOTTOM -> Pair(0.45f, 0.95f)     // Lower half (legs) - overlaps TOP
            ClothingCategory.TOP -> Pair(0.05f, 0.55f)        // Upper half (torso) - overlaps BOTTOM
            ClothingCategory.OUTERWEAR -> Pair(0.05f, 0.70f)  // Covers torso + some legs
            ClothingCategory.ACCESSORIES -> Pair(0.0f, 0.20f) // Very top area
            ClothingCategory.DRESS -> Pair(0.05f, 0.95f)      // Spans most of canvas
            ClothingCategory.OTHER -> Pair(0.25f, 0.75f)      // Middle area
        }
    }

    /**
     * Feathers (softens) the alpha edges of a bitmap to remove hard rectangular outlines.
     * This creates a soft matte effect that prevents blocky 3D extrusion.
     *
     * @param bitmap Source bitmap with alpha channel
     * @return New bitmap with feathered alpha edges
     */
    private fun featherAlphaEdges(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        Log.d(TAG, "Feathering alpha edges (radius=$FEATHER_RADIUS)...")

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Extract alpha values
        val alphaValues = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                alphaValues[y][x] = (pixel shr 24) and 0xFF
            }
        }

        // Apply box blur to alpha channel (approximates Gaussian)
        val blurredAlpha = boxBlurAlpha(alphaValues, width, height, FEATHER_RADIUS)

        // Apply blurred alpha to output
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val newAlpha = blurredAlpha[y][x]

                val newPixel = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
                output.setPixel(x, y, newPixel)
            }
        }

        Log.d(TAG, "Alpha edges feathered")
        return output
    }

    /**
     * Applies box blur to alpha channel (fast approximation of Gaussian blur).
     */
    private fun boxBlurAlpha(alpha: Array<IntArray>, width: Int, height: Int, radius: Int): Array<IntArray> {
        val output = Array(height) { IntArray(width) }

        // Horizontal pass
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                for (dx in -radius..radius) {
                    val nx = x + dx
                    if (nx in 0 until width) {
                        sum += alpha[y][nx]
                        count++
                    }
                }
                output[y][x] = sum / count
            }
        }

        // Vertical pass
        val finalOutput = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                for (dy in -radius..radius) {
                    val ny = y + dy
                    if (ny in 0 until height) {
                        sum += output[ny][x]
                        count++
                    }
                }
                finalOutput[y][x] = sum / count
            }
        }

        return finalOutput
    }

    /**
     * Merges multiple item silhouettes into a single connected shape using morphological closing.
     * This eliminates gaps between clothing items and creates one unified silhouette.
     *
     * Morphological closing = dilation followed by erosion.
     * This bridges small gaps while preserving overall shape.
     *
     * @param bitmap Composite bitmap with multiple items
     * @return New bitmap with merged silhouette
     */
    private fun mergeIntoConnectedSilhouette(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        Log.d(TAG, "Merging items into connected silhouette (morph radius=$MORPH_RADIUS)...")

        // Extract alpha channel
        val alpha = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                alpha[y][x] = (pixel shr 24) and 0xFF
            }
        }

        // Morphological dilation (expand)
        val dilated = morphologicalDilate(alpha, width, height, MORPH_RADIUS)

        // Morphological erosion (shrink back)
        val closed = morphologicalErode(dilated, width, height, MORPH_RADIUS)

        // Apply closed alpha mask to output
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val newAlpha = closed[y][x]

                val newPixel = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
                output.setPixel(x, y, newPixel)
            }
        }

        Log.d(TAG, "Silhouettes merged into single connected shape")
        return output
    }

    /**
     * Morphological dilation - expands opaque regions.
     */
    private fun morphologicalDilate(alpha: Array<IntArray>, width: Int, height: Int, radius: Int): Array<IntArray> {
        val output = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                var maxAlpha = 0
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            // Circular kernel
                            if (dx * dx + dy * dy <= radius * radius) {
                                maxAlpha = maxOf(maxAlpha, alpha[ny][nx])
                            }
                        }
                    }
                }
                output[y][x] = maxAlpha
            }
        }

        return output
    }

    /**
     * Morphological erosion - shrinks opaque regions.
     */
    private fun morphologicalErode(alpha: Array<IntArray>, width: Int, height: Int, radius: Int): Array<IntArray> {
        val output = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                var minAlpha = 255
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            // Circular kernel
                            if (dx * dx + dy * dy <= radius * radius) {
                                minAlpha = minOf(minAlpha, alpha[ny][nx])
                            }
                        }
                    }
                }
                output[y][x] = minAlpha
            }
        }

        return output
    }

    /**
     * Crops transparent margins from a composite bitmap and scales to fit canvas.
     *
     * Finds the tight bounding box of all non-transparent pixels, crops to that region,
     * then scales back to fit within the canvas while preserving aspect ratio and centering.
     *
     * This removes empty space around the outfit, reducing the "slab" effect in 3D models.
     *
     * @param bitmap Composite bitmap with potential transparent margins
     * @return New bitmap cropped and scaled to fit canvas
     */
    private fun cropTransparentMargins(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        Log.d(TAG, "Scanning for content bounding box...")

        // Find bounding box of non-transparent pixels
        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0
        var hasContent = false

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel shr 24) and 0xFF

                // If pixel is not fully transparent
                if (alpha > 0) {
                    hasContent = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // If no content found, return original
        if (!hasContent) {
            Log.w(TAG, "No non-transparent content found, returning original bitmap")
            return bitmap
        }

        val contentWidth = maxX - minX + 1
        val contentHeight = maxY - minY + 1

        Log.d(TAG, "Content bounding box: ($minX,$minY) to ($maxX,$maxY), size ${contentWidth}x$contentHeight")

        // Crop to content
        val cropped = Bitmap.createBitmap(bitmap, minX, minY, contentWidth, contentHeight)

        // Scale to fit within canvas while preserving aspect ratio
        val scaleX = CANVAS_WIDTH.toFloat() / contentWidth
        val scaleY = CANVAS_HEIGHT.toFloat() / contentHeight
        val scale = minOf(scaleX, scaleY)

        val scaledWidth = (contentWidth * scale).toInt()
        val scaledHeight = (contentHeight * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(cropped, scaledWidth, scaledHeight, true)
        cropped.recycle()

        // Center on canvas with transparent background
        val final = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(final)
        canvas.drawColor(Color.TRANSPARENT)

        val left = (CANVAS_WIDTH - scaledWidth) / 2
        val top = (CANVAS_HEIGHT - scaledHeight) / 2

        canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)
        scaled.recycle()

        Log.d(TAG, "Cropped and scaled to ${scaledWidth}x$scaledHeight, centered on canvas")

        return final
    }

    /**
     * Saves the composite bitmap to cache directory as PNG.
     *
     * @param bitmap Composite bitmap to save
     * @param context Application context
     * @return File object for saved composite
     * @throws IOException if save fails
     */
    private fun saveCompositeToCache(bitmap: Bitmap, context: Context): File {
        val cacheDir = File(context.cacheDir, COMPOSITE_CACHE_DIR)

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val fileName = "outfit_${UUID.randomUUID()}.png"
        val file = File(cacheDir, fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            Log.d(
                TAG,
                "Saved composite to: ${file.absolutePath} (${file.length() / 1024} KB)"
            )

            return file

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save composite", e)
            throw IOException("Failed to save composite image: ${e.message}")
        }
    }

    /**
     * Cleans up old composite images from cache directory.
     * Call this periodically to prevent cache bloat.
     *
     * @param context Application context
     * @param maxAgeMillis Maximum age in milliseconds (default: 7 days)
     */
    fun cleanupOldComposites(
        context: Context,
        maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS
    ) {
        try {
            val cacheDir = File(context.cacheDir, COMPOSITE_CACHE_DIR)

            if (!cacheDir.exists()) {
                return
            }

            val currentTime = System.currentTimeMillis()
            val files = cacheDir.listFiles() ?: return

            var deletedCount = 0
            var freedBytes = 0L

            for (file in files) {
                val age = currentTime - file.lastModified()

                if (age > maxAgeMillis) {
                    val size = file.length()
                    if (file.delete()) {
                        deletedCount++
                        freedBytes += size
                    }
                }
            }

            if (deletedCount > 0) {
                Log.d(
                    TAG,
                    "Cleaned up $deletedCount old composites (freed ${freedBytes / 1024} KB)"
                )
            }

        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old composites", e)
            // Non-fatal, just log
        }
    }
}
