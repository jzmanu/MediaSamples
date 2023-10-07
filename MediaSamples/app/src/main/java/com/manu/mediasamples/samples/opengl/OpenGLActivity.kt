package com.manu.mediasamples.samples.opengl

import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.manu.mediasamples.databinding.ActivityOpenGLBinding
import com.manu.mediasamples.util.L

/**
 * openGL渲染视频
 */
class OpenGLActivity : AppCompatActivity() {
    companion object{
        const val TAG = "OpenGLActivity"
    }
    private lateinit var binding: ActivityOpenGLBinding
    private lateinit var playRenderer: PlayRenderer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityOpenGLBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.glSurfaceView.setEGLContextClientVersion(2)
        playRenderer = PlayRenderer(this, binding.glSurfaceView)
        binding.glSurfaceView.setRenderer(playRenderer)
        binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onDestroy() {
        playRenderer.destroy()
        super.onDestroy()
    }

    fun test(view: View) {
        playRenderer.stop()
    }
}