package com.manu.mediasamples.samples.record

import android.media.AudioRecord
import com.manu.mediasamples.util.L
import java.util.concurrent.LinkedBlockingQueue
import kotlin.properties.Delegates

/**
 * @Desc:
 * @Author: jzman
 * @Date: 2021/4/28 22:45.
 */
class AudioThread {
    private val tag = "AudioThread"
    private var bufferSize by Delegates.notNull<Int>()
    private var quene: LinkedBlockingQueue<ByteArray> = LinkedBlockingQueue()

    /** 录制状态 -1表示默认状态，1表述录制状态，0表示停止录制*/
    private var recording = -1

    init {
        bufferSize = AudioRecord.getMinBufferSize(
            RecordConfig.SAMPLE_RATE,
            RecordConfig.CHANNEL_CONFIG,
            RecordConfig.AUDIO_FORMAT
        )
    }

    private val mAudioRecord: AudioRecord by lazy {
        AudioRecord(
            RecordConfig.AUDIO_SOURCE,
            RecordConfig.SAMPLE_RATE,
            RecordConfig.CHANNEL_CONFIG,
            RecordConfig.AUDIO_FORMAT,
            bufferSize
        )
    }

    /**
     * 开始录制
     */
    fun startRecord(){
        L.i(tag, "startAudioRecord")
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            L.i(tag, "参数异常")
            return;
        }
        recording = 1;
        mAudioRecord.startRecording()
        mAudioRecord.recordingState
        Thread(RecordRunnable()).start();
    }

    /**
     * 结束录制
     */
    fun stopRecord(){
        mAudioRecord.stop()
        recording = 0
    }

    /**
     * 获取音频数据
     */
    fun poll():ByteArray?{
        return quene.poll()
    }

    /**
     * 录制Runnable
     */
    inner class RecordRunnable : Runnable{

        override fun run() {
            val byteArray = ByteArray(bufferSize)
            while (recording == 1){
                val result = mAudioRecord.read(byteArray, 0, bufferSize)
                if (result > 0){
                    val resultArray = ByteArray(result)
                    System.arraycopy(byteArray, 0, resultArray, 0, result)
                    quene.offer(resultArray)
                }
            }
            // 自定义流结束的数据
            if (recording == 0){
                val stopArray = byteArrayOf((-100).toByte())
                quene.offer(stopArray)
            }
        }
    }
}