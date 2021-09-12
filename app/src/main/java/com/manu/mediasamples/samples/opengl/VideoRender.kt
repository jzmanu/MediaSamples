package com.manu.mediasamples.samples.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import com.manu.mediasamples.R
import com.manu.mediasamples.util.GLUtil
import com.manu.mediasamples.util.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.properties.Delegates

/**
 * @Desc:
 * @Author: Administrator
 * @Date: 2021/8/5 14:56.
 */
class VideoRender(private var context: Context) : IRender,SurfaceTexture.OnFrameAvailableListener {
    companion object{
        private const val TAG = "VideoRender"
    }
    private var textureId: Int = TextureHelper.createOESTexture()
    private var surfaceTexture: SurfaceTexture = SurfaceTexture(textureId)
    private var programHandler by Delegates.notNull<Int>()

    private var vertexPositionHandler = -1
    private var texturePositionHandler = -1
    private var textureHandler = -1;
    private var frameUpdate:Boolean = false
    var onNotifyFrameUpdateListener: OnNotifyFrameUpdateListener? = null

    // 顶点坐标
    private val vertexCoordinates = floatArrayOf(
        1.0f, 1.0f,
        -1.0f, 1.0f,
        -1.0f, -1.0f,
        1.0f, -1.0f
    )

    // 纹理坐标
    private val textureCoordinates = floatArrayOf(
        1.0f, 0.0f,
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    )

    private var vertexBuffer =
        ByteBuffer.allocateDirect(vertexCoordinates.size * 4).run {
        this.order(ByteOrder.nativeOrder())
        this.asFloatBuffer().apply {
            put(vertexCoordinates)
            position(0)
        }
    }

    private var textureBuffer =
        ByteBuffer.allocateDirect(textureCoordinates.size * 4).run {
        this.order(ByteOrder.nativeOrder())
        this.asFloatBuffer().apply {
            put(textureCoordinates)
            position(0)
        }
    }

    init {
        // init Texture id
        surfaceTexture.setOnFrameAvailableListener(this)
        // read shader source code
        val vertexShaderCode =
            GLUtil.readShaderSourceCodeFromRaw(context, R.raw.video_vertex_shader_default)
        val fragmentShaderCode =
            GLUtil.readShaderSourceCodeFromRaw(context, R.raw.video_fragment_shader_default)
        // check
        if (vertexShaderCode.isNullOrEmpty() || fragmentShaderCode.isNullOrEmpty()) {
            throw RuntimeException("vertexShaderCode or fragmentShaderCode is null or empty")
        }
        // compile shader
        val vertexShaderHandler = GLUtil.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShaderHandler =
            GLUtil.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        // use Program
        programHandler = GLUtil.createAndLinkProgram(vertexShaderHandler, fragmentShaderHandler)
        GLES20.glUseProgram(programHandler)
    }

    override fun draw() {
        // render
        surfaceTexture.updateTexImage()
        // get shader handler
        vertexPositionHandler = GLES20.glGetAttribLocation(programHandler, "aPosition")
        texturePositionHandler = GLES20.glGetAttribLocation(programHandler, "aCoordinate")
        textureHandler = GLES20.glGetUniformLocation(programHandler, "vTextureCoordinate")
        // enable vertex
        GLES20.glEnableVertexAttribArray(vertexPositionHandler)
        GLES20.glEnableVertexAttribArray(texturePositionHandler)
        // set
        GLES20.glVertexAttribPointer(vertexPositionHandler,2,GLES20.GL_FLOAT,false,0, vertexBuffer)
        GLES20.glVertexAttribPointer(texturePositionHandler,2,GLES20.GL_FLOAT,false,0, textureBuffer)
        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        // disable
        GLES20.glDisableVertexAttribArray(vertexPositionHandler)
        GLES20.glDisableVertexAttribArray(texturePositionHandler)
    }

    override fun getSurface(): Surface {
        return Surface(surfaceTexture)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        Log.d(TAG,"onFrameAvailable")
        onNotifyFrameUpdateListener?.onNotifyUpdate()
    }

    interface OnNotifyFrameUpdateListener{
        fun onNotifyUpdate();
    }
}