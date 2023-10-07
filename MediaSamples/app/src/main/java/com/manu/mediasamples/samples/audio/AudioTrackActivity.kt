package com.manu.mediasamples.samples.audio

import android.media.*
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.manu.mediasamples.R
import com.manu.mediasamples.databinding.ActivityTrackAudioBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import kotlin.properties.Delegates

/**
 * AudioTrack
 */
class AudioTrackActivity : AppCompatActivity(),View.OnClickListener{

    companion object {
        const val TAG = "AudioTrackActivity"

        /** 音频格式 */
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_8BIT

        /** 采样率 */
        const val SAMPLE_RATE = 8000

        /** 声道配置 */
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    }

    private lateinit var binding:ActivityTrackAudioBinding
    private val scope = MainScope()

    private lateinit var audioTrack: AudioTrack
    private lateinit var attributes: AudioAttributes
    private lateinit var audioFormat: AudioFormat
    private var bufferSize by Delegates.notNull<Int>()
    private lateinit var pcmFilePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPlay.setOnClickListener(this)
        binding.btnStop.setOnClickListener(this)
        binding.btnPause.setOnClickListener(this)

        pcmFilePath = Environment.getExternalStorageDirectory()
            .path + File.separator + "test.pcm"
        Log.i(TAG, "pcmFilePath:$pcmFilePath")
        initAudioTrack()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btnPlay -> start()
            R.id.btnStop -> audioTrack.stop()
            R.id.btnPause -> audioTrack.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioTrack.release()
    }

    private fun start(){
        audioTrack.play()
        writeAudioData()
    }

    private fun initAudioTrack() {
        bufferSize = AudioTrack
            .getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA) // 设置音频的用途
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) // 设置音频的内容类型
            .build()
        audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        audioTrack = AudioTrack(
            attributes, audioFormat, bufferSize,
            AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private fun writeAudioData(){
        scope.launch(Dispatchers.IO){
            val pcmFile = File(pcmFilePath)
            val ins = FileInputStream(pcmFile)
            val bytes = ByteArray(bufferSize)
            var len: Int
            while (ins.read(bytes).also { len = it } > 0){
                audioTrack.write(bytes, 0, len)
            }
            audioTrack.stop()
        }
    }
}