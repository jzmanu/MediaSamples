package com.manu.mediasamples.samples.opengl

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.manu.mediasamples.util.L
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @Desc: PlayRenderer
 * @Author: jzman
 * @Date: 2021/8/2.
 */
class PlayRenderer(private var context: Context,private var glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer,
    VideoRender.OnNotifyFrameUpdateListener{
    companion object {
        private const val TAG = "PlayRenderer"
    }

    private lateinit var videoRender: VideoRender
    private var mediaPlayer: MediaPlayer = MediaPlayer()

    init {
        mediaPlayer.setDataSource("http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4")
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
        }
        mediaPlayer.setOnErrorListener((MediaPlayer.OnErrorListener { mp, what, extra ->
            L.i(TAG, "error > what:$what,extra:$extra")
            return@OnErrorListener true
        }))
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        L.i(TAG, "onSurfaceCreated")
        videoRender = VideoRender(context)
        videoRender.onNotifyFrameUpdateListener = this
        mediaPlayer.setSurface(videoRender.getSurface())
    }

    @SuppressLint("NewApi")
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        L.i(TAG, "onSurfaceChanged > width:$width,height:$height")
        GLES20.glViewport(0, 0, width, height)
        mediaPlayer.prepareAsync()
    }

    override fun onDrawFrame(gl: GL10) {
        L.i(TAG, "onDrawFrame")
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        videoRender.draw()
    }

    fun destroy(){
        mediaPlayer.reset()
        mediaPlayer.release()
    }

    override fun onNotifyUpdate() {
        glSurfaceView.requestRender()
    }
}