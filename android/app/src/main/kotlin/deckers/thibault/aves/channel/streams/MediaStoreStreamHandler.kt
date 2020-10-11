package deckers.thibault.aves.channel.streams

import android.content.Context
import android.os.Handler
import android.os.Looper
import deckers.thibault.aves.model.provider.MediaStoreImageProvider
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink

class MediaStoreStreamHandler(private val context: Context, arguments: Any?) : EventChannel.StreamHandler {
    private lateinit var eventSink: EventSink
    private lateinit var handler: Handler
    private var knownEntries: Map<Int, Int>? = null

    init {
        if (arguments is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            knownEntries = arguments["knownEntries"] as Map<Int, Int>?
        }
    }

    override fun onListen(arguments: Any?, eventSink: EventSink) {
        this.eventSink = eventSink
        handler = Handler(Looper.getMainLooper())
        Thread { fetchAll() }.start()
    }

    override fun onCancel(arguments: Any?) {}

    private fun success(result: Map<String, Any>) {
        handler.post { eventSink.success(result) }
    }

    private fun endOfStream() {
        handler.post { eventSink.endOfStream() }
    }

    private fun fetchAll() {
        MediaStoreImageProvider().fetchAll(context, knownEntries) { success(it) }
        endOfStream()
    }

    companion object {
        const val CHANNEL = "deckers.thibault/aves/mediastorestream"
    }
}