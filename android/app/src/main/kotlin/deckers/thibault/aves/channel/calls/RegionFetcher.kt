package deckers.thibault.aves.channel.calls

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import deckers.thibault.aves.utils.MimeTypes
import deckers.thibault.aves.utils.StorageUtils
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

class RegionFetcher internal constructor(
    private val context: Context,
) {
    private var lastDecoderRef: LastDecoderRef? = null

    fun fetch(
        uri: Uri,
        mimeType: String,
        sampleSize: Int,
        regionRect: Rect,
        imageSize: Size,
        result: MethodChannel.Result,
    ) {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        var currentDecoderRef = lastDecoderRef
        if (currentDecoderRef != null && currentDecoderRef.uri != uri) {
            currentDecoderRef.decoder.recycle()
            currentDecoderRef = null
        }

        try {
            if (currentDecoderRef == null) {
                val newDecoder = StorageUtils.openInputStream(context, uri).use { input ->
                    BitmapRegionDecoder.newInstance(input, false)
                }
                currentDecoderRef = LastDecoderRef(uri, newDecoder)
            }
            val decoder = currentDecoderRef.decoder
            lastDecoderRef = currentDecoderRef

            // with raw images, the known image size may not match the decoded image size
            // so we scale the requested region accordingly
            val effectiveRect = if (imageSize.width != decoder.width || imageSize.height != decoder.height) {
                val xf = decoder.width.toDouble() / imageSize.width
                val yf = decoder.height.toDouble() / imageSize.height
                Rect(
                    (regionRect.left * xf).roundToInt(),
                    (regionRect.top * yf).roundToInt(),
                    (regionRect.right * xf).roundToInt(),
                    (regionRect.bottom * yf).roundToInt(),
                )
            } else {
                regionRect
            }

            val data = decoder.decodeRegion(effectiveRect, options)?.let {
                val stream = ByteArrayOutputStream()
                // we compress the bitmap because Dart Image.memory cannot decode the raw bytes
                // Bitmap.CompressFormat.PNG is slower than JPEG, but it allows transparency
                if (MimeTypes.canHaveAlpha(mimeType)) {
                    it.compress(Bitmap.CompressFormat.PNG, 0, stream)
                } else {
                    it.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                stream.toByteArray()
            }
            if (data != null) {
                result.success(data)
            } else {
                result.error("getRegion-null", "failed to decode region for uri=$uri regionRect=$regionRect", null)
            }
        } catch (e: Exception) {
            result.error("getRegion-read-exception", "failed to initialize region decoder for uri=$uri regionRect=$regionRect", e.message)
        }
    }
}

private data class LastDecoderRef(
    val uri: Uri,
    val decoder: BitmapRegionDecoder,
)
