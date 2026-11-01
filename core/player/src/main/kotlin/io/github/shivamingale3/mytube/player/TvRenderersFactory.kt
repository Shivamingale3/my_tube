package io.github.shivamingale3.mytube.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener

/**
 * Renderers tuned for TV chips that undersell themselves.
 *
 * Amlogic's VP9 decoder, for example, declares 3840×2160 but only enough blocks per second for
 * 30 fps, so Media3 would skip 4K60 even though the hardware plays it (SmartTube does). Formats
 * within the decoder's declared size are therefore allowed past its declared speed; their IDs are
 * collected in [lifted] for the stats panel. HDR formats are left alone.
 */
@OptIn(UnstableApi::class)
internal class TvRenderersFactory(context: Context, private val lifted: MutableSet<String>) : DefaultRenderersFactory(context) {

    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>,
    ) {
        super.buildVideoRenderers(
            context, extensionRendererMode, mediaCodecSelector, enableDecoderFallback,
            eventHandler, eventListener, allowedVideoJoiningTimeMs, out,
        )
        val index = out.indexOfFirst { it is MediaCodecVideoRenderer }
        if (index >= 0) {
            out[index] = LiftedVideoRenderer(
                context, mediaCodecSelector, allowedVideoJoiningTimeMs, enableDecoderFallback,
                eventHandler, eventListener, MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY, lifted,
            )
        }
    }

    // Captions loaded beside the video (not from a DASH manifest) arrive as raw WebVTT/TTML samples.
    override fun buildTextRenderers(context: Context, output: TextOutput, outputLooper: Looper, extensionRendererMode: Int, out: ArrayList<Renderer>) {
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out)
        out.filterIsInstance<TextRenderer>().forEach { it.experimentalSetLegacyDecodingEnabled(true) }
    }
}

@OptIn(UnstableApi::class)
@Suppress("DEPRECATION")
private class LiftedVideoRenderer(
    context: Context,
    selector: MediaCodecSelector,
    joiningTimeMs: Long,
    decoderFallback: Boolean,
    handler: Handler,
    listener: VideoRendererEventListener,
    maxDroppedFrames: Int,
    private val lifted: MutableSet<String>,
) : MediaCodecVideoRenderer(context, selector, joiningTimeMs, decoderFallback, handler, listener, maxDroppedFrames) {

    override fun supportsFormat(mediaCodecSelector: MediaCodecSelector, format: Format): Int {
        val capabilities = super.supportsFormat(mediaCodecSelector, format)
        if (RendererCapabilities.getFormatSupport(capabilities) != C.FORMAT_EXCEEDS_CAPABILITIES) return capabilities
        val long = maxOf(format.width, format.height)
        val short = minOf(format.width, format.height)
        val hdr = format.colorInfo?.colorTransfer.let { it == C.COLOR_TRANSFER_ST2084 || it == C.COLOR_TRANSFER_HLG }
        if (short <= 0 || long > MAX_LONG_SIDE || short > MAX_SHORT_SIDE || hdr) return capabilities
        format.id?.let(lifted::add)
        return (capabilities and RendererCapabilities.FORMAT_SUPPORT_MASK.inv()) or C.FORMAT_HANDLED
    }

    private companion object {
        const val MAX_LONG_SIDE = 3840
        const val MAX_SHORT_SIDE = 2160
    }
}
