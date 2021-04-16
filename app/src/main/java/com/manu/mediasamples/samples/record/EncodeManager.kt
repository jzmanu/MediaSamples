package com.manu.mediasamples.samples.record

import android.media.MediaMuxer
import android.util.Log
import com.manu.mediasamples.app.MediaApplication
import java.io.IOException

/**
 * @Desc:
 * @Author: jzman
 */
object EncodeManager {
    private const val TAG = "EncodeManager"
    private lateinit var mMediaMuxer: MediaMuxer

    fun init(width: Int, height: Int){
        initMuxer()
        AudioEncode.initAudio(mMediaMuxer)
        VideoEncode.initVideo(width,height, mMediaMuxer)
    }

    /**
     * start
     */
    fun startEncode(){
        AudioEncode.startAudioRecord()
        AudioEncode.startAudioEncode()
        VideoEncode.startVideoEncode()
    }

    /**
     * stop
     */
    fun stopEncode(){
        VideoEncode.stopVideoEncode()
        AudioEncode.stopAudioEncode()
        mMediaMuxer.stop()
    }

    /**
     * 初始化MediaMuxer
     */
    private fun initMuxer() {
        Log.d(TAG, "initMuxer start")
        try {
            val path = "${MediaApplication.context.filesDir}/test.mp4"
            mMediaMuxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            Log.e(
                TAG,
                "initMuxer fail: ${e.message}"
            )
        }
    }
}