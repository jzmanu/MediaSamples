package com.manu.mediasamples.samples.opengl

import android.content.Context
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Environment
import com.manu.mediasamples.util.L
import com.manu.mediasamples.util.TextureHelper
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * @Desc: PlayRenderer
 * @Author: jzman
 * @Date: 2021/8/2.
 */
class PlayRenderer(
    private var context: Context,
    private var glSurfaceView: GLSurfaceView
) : GLSurfaceView.Renderer,
    VideoRender.OnNotifyFrameUpdateListener, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener {
    companion object {
        private const val TAG = "PlayRenderer"
    }
    private lateinit var videoRender: VideoRender
    private lateinit var mediaPlayer: MediaPlayer
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vPMatrix = FloatArray(16)

    private var screenWidth: Int = -1
    private var screenHeight: Int = -1
    private var videoWidth: Int = -1
    private var videoHeight: Int = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        L.i(TAG, "onSurfaceCreated")
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        videoRender = VideoRender(context)
        videoRender.setTextureID(TextureHelper.createTextureId())
        videoRender.onNotifyFrameUpdateListener = this
        initMediaPlayer()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        L.i(TAG, "onSurfaceChanged > width:$width,height:$height")
        screenWidth = width
        screenHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        L.i(TAG, "onDrawFrame")
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        videoRender.draw(vPMatrix)
    }

    override fun onPrepared(mp: MediaPlayer?) {
        L.i(OpenGLActivity.TAG, "onPrepared")
        mediaPlayer.start()
    }

    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
        L.i(OpenGLActivity.TAG, "onVideoSizeChanged > width:$width ,height:$height")
        this.videoWidth = width
        this.videoHeight = height
        if (!Config.DEFAULT) initMatrix()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        L.i(OpenGLActivity.TAG, "onCompletion")
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        L.i(OpenGLActivity.TAG, "error > what:$what,extra:$extra")
        return true
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnVideoSizeChangedListener(this)
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setDataSource("http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4")
//        mediaPlayer.setDataSource(Environment.getExternalStorageDirectory().absolutePath + "/video.mp4")
        mediaPlayer.setSurface(videoRender.getSurface())
        mediaPlayer.prepareAsync()
    }

    private fun initMatrix() {
        // 设置相机位置（视图矩阵）
        Matrix.setLookAtM(
            viewMatrix, 0,
            0.0f, 0.0f, 5.0f, // 相机位置
            0.0f, 0.0f, 0.0f, // 目标位置
            0.0f, 1.0f, 0.0f // 相机正上方向量
        )
        // 计算视频缩放比例（投影矩阵）
        val screenRatio = screenWidth / screenHeight.toFloat()
        val videoRatio = videoWidth / videoHeight.toFloat()
        val ratio: Float
        if (screenWidth >= screenHeight) {
            if (videoRatio > screenRatio) {
                ratio = videoRatio / screenRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -1f, 1f, -ratio, ratio, -1f, 5f
                )
            } else {
                ratio = screenRatio / videoRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -ratio, ratio, -1f, 1f, -1f, 5f
                )
            }
        } else {
            if (videoRatio >= screenRatio) {
                ratio = videoRatio / screenRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -1f, 1f, -ratio, ratio, -1f, 5f
                )
            } else {
                ratio = screenRatio / videoRatio
                Matrix.orthoM(
                    projectionMatrix, 0,
                    -ratio, ratio, -1f, 1f, -1f, 5f
                )
            }
        }
        // 计算投影和视图变换
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        L.i(TAG, "initMatrix > screenRatio:$screenRatio,videoRatio:$videoRatio,ratio:$ratio")
    }

    override fun onNotifyUpdate() {
        glSurfaceView.requestRender()
    }

    fun destroy() {
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    fun stop(){
        mediaPlayer.stop()
    }
}