package com.manu.mediasamples.samples.record

import android.media.*
import android.os.Build
import android.util.Log
import com.manu.mediasamples.util.L
import java.nio.ByteBuffer
import java.util.*
import kotlin.properties.Delegates

/**
 *
 * @Desc: AudioEncode
 * @Author: jzman
 */
object AudioEncode2 : MediaCodec.Callback() {
    private const val TAG = "AudioEncode2"
    /** 录音源为主麦克风 */
    private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    /** 音频格式 */
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    /** 采样率 */
    private const val SAMPLE_RATE = 44100
    /** 声道配置 */
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO

    private var bufferSize by Delegates.notNull<Int>()
    private lateinit var mAudioCodec: MediaCodec
    private lateinit var mAudioMuxer: MediaMuxer
    private lateinit var mAudioRecord: AudioRecord

    private var pts: Long = 0
    private var isAudioStreamEnd = false;
    private lateinit var mAudioThread:AudioThread

    fun initAudio(muxer: MediaMuxer) {
        L.i(TAG, "initAudio")
        bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        this.mAudioMuxer = muxer
        initAudioCodec()
        mAudioThread = AudioThread()
    }

    /**
     * 开始编码
     */
    fun startAudioEncode() {
        L.i(TAG, "startEncode")
        mAudioCodec.start()
//        mAudioThread.startRecord()
        startAudioRecord()

    }


    /**
     * 结束编码
     */
    fun stopAudioEncode() {
        L.i(TAG, "stopEncode")
        mAudioCodec.stop()
        mAudioThread.stopRecord()
    }

    private fun initAudioCodec() {
        L.i(TAG, "init Codec start")
        try {
            val mediaFormat =
                MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, RecordConfig.SAMPLE_RATE, 2)
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
        if (!RecordConfig.isMuxerStart){
            mAudioCodec.releaseOutputBuffer(index, false)
            return
        }
        val outputBuffer = codec.getOutputBuffer(index) ?: return
        if (info.size > 0 && RecordConfig.isMuxerStart) {
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
        Log.d(TAG,"onInputBufferAvailable index:$index")
        val byteArray = ByteArray(bufferSize)
        when (val result = mAudioRecord.read(byteArray, 0, bufferSize)) {
            AudioRecord.ERROR_INVALID_OPERATION -> {
                Log.i(TAG,"ERROR_INVALID_OPERATION")
            }
            AudioRecord.ERROR_BAD_VALUE -> {
                Log.i(TAG,"ERROR_BAD_VALUE")
            }
            AudioRecord.ERROR_DEAD_OBJECT -> {
                Log.i(TAG,"ERROR_DEAD_OBJECT")
            }
            AudioRecord.ERROR -> {
                Log.i(TAG,"ERROR")
            }
            else -> {
                encodePcmSource(index, byteArray, result)
            }
        }
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        L.i(TAG, "onOutputFormatChanged format:${format}")
        RecordConfig.audioTrackIndex = mAudioMuxer.addTrack(format)
        L.i(TAG, "onOutputFormatChanged mAudioTrackIndex:${RecordConfig.audioTrackIndex}")
        if (RecordConfig.videoTrackIndex != -1) {
            mAudioMuxer.start()
            RecordConfig.isMuxerStart = true
            L.i(TAG, "onOutputFormatChanged isMuxerStart:${RecordConfig.isMuxerStart}")
        }
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        L.i(TAG, "onError e:${e.message}")
    }

    fun startAudioRecord(){
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE){
            Log.i(TAG,"参数异常")
            return;
        }

        mAudioRecord = AudioRecord(
            AUDIO_SOURCE,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )
        mAudioRecord.startRecording()
    }

    private fun encodePcmSource(buffIndex:Int,pcmBuffer: ByteArray, buffSize: Int) {
        try {

            val byteBuffer = mAudioCodec.getInputBuffer(buffIndex) ?: return
            byteBuffer.clear()
            byteBuffer.put(pcmBuffer)
            // presentationTimeUs = 1000000L * (buffSize / 2) / sampleRate
            // 一帧音频帧大小 int size = 采样率 x 位宽 x 采样时间 x 通道数
            // 1s时间戳计算公式  presentationTimeUs = 1000000L * (totalBytes / sampleRate/ audioFormat / channelCount / 8 )
            // totalBytes : 传入编码器的总大小
            // 1000 000L : 单位为 微秒，换算后 = 1s,
            //除以8     : pcm原始单位是bit, 1 byte = 8 bit, 1 short = 16 bit, 用 Byte[]、Short[] 承载则需要进行换算
//            pts += (1.0 * buffSize / (SAMPLE_RATE * 2 * (AUDIO_FORMAT / 8)) * 1000000.0).toLong()
            pts = System.nanoTime() / 1000;
            Log.i(TAG,
                "pcm一帧时间戳 = " + pts / 1000000.0f
            )
            mAudioCodec.queueInputBuffer(buffIndex, 0, buffSize, pts, 0)
        } catch (e: Exception) {
            //audioCodec 线程对象已释放MediaCodec对象
            Log.i(TAG,"encodePcmSource: ${e.message}")
        }
    }
}