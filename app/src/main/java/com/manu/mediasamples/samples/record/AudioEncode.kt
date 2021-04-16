package com.manu.mediasamples.samples.record

import android.media.*
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

/**
 * @Desc: AudioEncode
 * @Author: jzman
 */
object AudioEncode : MediaCodec.Callback() {
    private const val TAG = "AudioEncode"
    /** 录音源为主麦克风 */
    private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    /** 音频格式 */
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    /** 采样率 */
    private const val SAMPLE_RATE = 44100
    /** 声道配置 */
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO

    private var bufferSize by Delegates.notNull<Int>()

    private lateinit var mAudioRecord: AudioRecord
    private lateinit var mAudioCodec: MediaCodec
    private lateinit var mAudioMuxer: MediaMuxer

    private var presentationTimeUs:Long = 0
    var mAudioTrackIndex = -1

    fun initAudio(muxer: MediaMuxer){
        Log.d(TAG, "initAudio")
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        this.mAudioMuxer = muxer
        initAudioCodec()
    }

    /**
     * 开始编码
     */
    fun startAudioEncode() {
        Log.d(TAG, "startEncode")
        mAudioCodec.start()
    }

    /**
     * 结束编码
     */
    fun stopAudioEncode() {
        Log.d(TAG, "stopEncode")
        mAudioCodec.stop()
    }

    private fun initAudioCodec() {
        Log.i(TAG,"init Codec start")
        try {
            val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 2)
            mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
            mediaFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
            mAudioCodec.setCallback(this)
            mAudioCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }catch (e:Exception){
            Log.i(TAG,"init error:${e.message}")
        }
        Log.i(TAG,"init Codec end")
    }

    fun startAudioRecord(){
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE){
            Log.i(TAG,"参数异常")
            return;
        }

        mAudioRecord = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
        mAudioRecord.startRecording()

//        CoroutineScope(Dispatchers.IO).launch{
//            while (true){
//                val byteArray = ByteArray(bufferSize)
//                when (val result = mAudioRecord.read(byteArray, 0,bufferSize)) {
//                    AudioRecord.ERROR_INVALID_OPERATION -> {
//                        Log.i(TAG,"ERROR_INVALID_OPERATION")
//                    }
//                    AudioRecord.ERROR_BAD_VALUE -> {
//                        Log.i(TAG,"ERROR_BAD_VALUE")
//                    }
//                    AudioRecord.ERROR_DEAD_OBJECT -> {
//                        Log.i(TAG,"ERROR_DEAD_OBJECT")
//                    }
//                    AudioRecord.ERROR -> {
//                        Log.i(TAG,"ERROR")
//                    }
//                    else -> {
//                        encodePcmSource(byteArray,result)
//                    }
//                }
//            }
//        }
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
            presentationTimeUs += (1.0 * buffSize / (SAMPLE_RATE * 2 * (AUDIO_FORMAT / 8)) * 1000000.0).toLong()
            Log.i(TAG,
                "pcm一帧时间戳 = " + presentationTimeUs / 1000000.0f
            )
            mAudioCodec.queueInputBuffer(buffIndex, 0, buffSize, presentationTimeUs, 0)
        } catch (e: Exception) {
            //audioCodec 线程对象已释放MediaCodec对象
            Log.i(TAG,"encodePcmSource: ${e.message}")
        }
    }

    override fun onOutputBufferAvailable(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        Log.d(TAG,"onOutputBufferAvailable index:$index, info->offset:${info.offset},size:${info.size},pts:${info.presentationTimeUs/1000000}")
        val outputBuffer = codec.getOutputBuffer(index) ?: return
        outputBuffer.position(info.offset)
        outputBuffer.limit(info.size)
        if (presentationTimeUs == 0L){
            info.presentationTimeUs = info.presentationTimeUs - presentationTimeUs
        }
        mAudioMuxer.writeSampleData(mAudioTrackIndex,outputBuffer,info)
        mAudioCodec.releaseOutputBuffer(index,false)
    }

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        Log.d(TAG,"onInputBufferAvailable index:$index")
        val byteArray = ByteArray(bufferSize)
        when (val result = mAudioRecord.read(byteArray, 0,bufferSize)) {
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
                encodePcmSource(index,byteArray,result)
            }
        }
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.d(TAG,"onOutputFormatChanged format:${format}")
        mAudioTrackIndex = mAudioMuxer.addTrack(format)
//        mAudioMuxer.start()
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.d(TAG,"onError e:${e.message}")
    }
}