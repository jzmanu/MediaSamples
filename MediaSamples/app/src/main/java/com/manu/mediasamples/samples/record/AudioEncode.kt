package com.manu.mediasamples.samples.record

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.widget.Toast
import com.manu.mediasamples.util.L

/**
 * @Desc: AudioEncode
 * @Author: jzman
 */
object AudioEncode : MediaCodec.Callback() {
    private const val TAG = "AudioEncode"
    private lateinit var mAudioCodec: MediaCodec
    private lateinit var mAudioMuxer: MediaMuxer

    private var pts: Long = 0
    private var isAudioStreamEnd = false;
    private lateinit var mAudioThread: AudioThread

    fun initAudio(muxer: MediaMuxer) {
        L.i(TAG, "initAudio")
        this.mAudioMuxer = muxer
        initAudioCodec()
        mAudioThread = AudioThread()
    }

    /**
     * 开始编码
     */
    fun startAudioEncode() {
        L.i(TAG, "startEncode > mAudioMuxer:$mAudioMuxer")
        mAudioCodec.start()
        mAudioThread.startRecord()
    }

    /**
     * 结束编码
     */
    fun stopAudioEncode() {
        L.i(TAG, "stopEncode")
        mAudioCodec.stop()
        mAudioCodec.release()
        mAudioThread.stopRecord()
        RecordConfig.isAudioStop = true
        if (RecordConfig.isVideoStop) {
            mAudioMuxer.stop()
            mAudioMuxer.release()
            RecordConfig.isAudioStop = false
        }
    }

    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        L.i(
            TAG,
            "onOutputBufferAvailable index:$index, info->offset:${info.offset},size:${info.size}" +
                    ",pts:${info.presentationTimeUs / 1000000} , isMuxerStart:${RecordConfig.isMuxerStart}"
        )
        // 如果发现MediaMuxer还未启动，则释放这个OutputBuffer
        if (!RecordConfig.isMuxerStart) {
            mAudioCodec.releaseOutputBuffer(index, false)
            return
        }
        val outputBuffer = codec.getOutputBuffer(index) ?: return
        if (info.size > 0) {
            outputBuffer.position(info.offset)
            outputBuffer.limit(info.size)
            if (pts == 0L) {
                info.presentationTimeUs = info.presentationTimeUs - pts
            }
            mAudioMuxer.writeSampleData(RecordConfig.audioTrackIndex, outputBuffer, info)
            mAudioCodec.releaseOutputBuffer(index, false)
        }
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        L.i(
            TAG,
            "onInputBufferAvailable index:$index  ,isMuxerStart:${RecordConfig.isMuxerStart}"
        )

        val inputBuffer = codec.getInputBuffer(index)
        val result = mAudioThread.poll()

        if (result != null && result.size == 1 && result[0] == (-100).toByte()) {
            isAudioStreamEnd = true
        }

        L.i(TAG, "result:$result , isAudioStreamEnd:$isAudioStreamEnd")
        if (result != null && !isAudioStreamEnd) {
            val readSize = result.size
            inputBuffer?.clear()
            inputBuffer?.limit(readSize)
            inputBuffer?.put(result, 0, readSize)
            pts = System.nanoTime() / 1000;
            L.i(
                TAG,
                "pcm一帧时间戳 = ${pts / 1000000.0f}---pts:$pts"
            )
            mAudioCodec.queueInputBuffer(index, 0, readSize, pts, 0)
        }

        //如果为null就不调用queueInputBuffer  回调几次后就会导致无可用InputBuffer，从而导致MediaCodec任务结束 只能写个配置文件
        if (result == null && !isAudioStreamEnd) {
            codec.queueInputBuffer(
                index,
                0,
                0,
                0,
                0
            )
        }

        if (isAudioStreamEnd) {
            codec.queueInputBuffer(
                index,
                0,
                0,
                0,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        }
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        L.i(TAG, "onOutputFormatChanged format:${format}")
        addAudioTrack(format)
        if (RecordConfig.videoTrackIndex != -1) {
            mAudioMuxer.start()
            RecordConfig.isMuxerStart = true
            L.i(TAG, "onOutputFormatChanged isMuxerStart:${RecordConfig.isMuxerStart}")
        }
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        L.i(TAG, "onError e:${e.message}")
    }

    private fun initAudioCodec() {
        L.i(TAG, "init Codec start")
        try {
            val mediaFormat =
                MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC,
                    RecordConfig.SAMPLE_RATE,
                    2
                )
            mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
            mediaFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
            mAudioCodec.setCallback(this)
            mAudioCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            L.i(TAG, "init error:${e.message}")
        }
        L.i(TAG, "init Codec end")
    }

    private fun addAudioTrack(format: MediaFormat) {
        L.i(TAG, "addAudioTrack format:${format}")
        RecordConfig.audioTrackIndex = mAudioMuxer.addTrack(format)
        RecordConfig.isAddAudioTrack = true
    }
}