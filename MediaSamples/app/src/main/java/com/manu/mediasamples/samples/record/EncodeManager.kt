package com.manu.mediasamples.samples.record

import android.media.MediaMuxer
import android.util.Log
import android.widget.Toast
import com.manu.mediasamples.app.MediaApplication
import com.manu.mediasamples.util.L
import java.io.IOException

/**
 * @Desc:
 * @Author: jzman
 */
object EncodeManager {
    private const val TAG = "EncodeManager"
    private lateinit var mMediaMuxer: MediaMuxer

    /**
     * 初始化
     */
    fun init(width: Int, height: Int){
        L.i(TAG, "init")
        initMuxer()
        AudioEncode.initAudio(mMediaMuxer)
        VideoEncode.initVideo(width,height, mMediaMuxer)
    }

    /**
     * start
     */
    fun startEncode(){
        L.i(TAG, "startEncode")
        AudioEncode.startAudioEncode()
        VideoEncode.startVideoEncode()
    }

    /**
     * stop
     */
    fun stopEncode(){
        L.i(TAG, "stopEncode")
        AudioEncode.stopAudioEncode()
        VideoEncode.stopVideoEncode()
        RecordConfig.isMuxerStart = false
        RecordConfig.audioTrackIndex = -1
        RecordConfig.videoTrackIndex = -1
    }

    /**
     * 初始化MediaMuxer
     */
    private fun initMuxer() {
        L.i(TAG, "initMuxer")
        try {
            val path = "${MediaApplication.context.filesDir}/test.mp4"
            mMediaMuxer = MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            Toast.makeText(RecordActivity.activity,"initMuxer error",Toast.LENGTH_LONG).show()
            Log.e(
                TAG,
                "initMuxer fail: ${e.message}"
            )
        }
    }
}