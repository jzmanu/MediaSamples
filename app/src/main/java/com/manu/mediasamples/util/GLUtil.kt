package com.manu.mediasamples.util

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * @author : jzman
 * @desc: GLUtil
 * @date: 2021/7/3 20:46.
 */
object GLUtil {
    private val TAG = GLUtil::class.java.simpleName
    private val sIdentityMatrix = FloatArray(16)
    fun identityMatrix(): FloatArray {
        return sIdentityMatrix
    }

    /**
     * 是否支持OpenGL ES2.0.
     *
     * @param context Context
     * @return true表示supported
     */
    fun supportOpenGLES2(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        return configurationInfo.reqGlEsVersion >= 0x20000
    }

    /**
     * GL操作错误检查
     *
     * @param operation operation
     */
    fun glCheck(operation: String) {
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e(TAG, operation + ": glError " + GLUtils.getEGLErrorString(error))
        }
    }

    /**
     * 从raw木raw目录读取着色程序
     *
     * @param context Context
     * @param resId   resId
     * @return shader Source Code
     */
    fun readShaderSourceCodeFromRaw(context: Context, resId: Int): String?{
        val inputStream = context.resources.openRawResource(resId)
        val inputStreamReader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(inputStreamReader)
        var nextLine: String?
        val body = StringBuilder()
        try {
            while (bufferedReader.readLine().also { nextLine = it } != null) {
                body.append(nextLine)
                body.append('\n')
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return body.toString()
    }

    /**
     * 编译着色器
     *
     * @param shaderType   Shader type，[GLES20.GL_VERTEX_SHADER]
     * @param shaderSource Shader Source Code
     * @return An OpenGL handle to the shader.
     */
    fun compileShader(shaderType: Int, shaderSource: String): Int {
        // 创建着色器
        var shaderHandle = GLES20.glCreateShader(shaderType)
        if (shaderHandle != 0) {
            // 替换着色器中的源代码
            GLES20.glShaderSource(shaderHandle, shaderSource)
            // 编译着色器
            GLES20.glCompileShader(shaderHandle)
            // 获取编译着色器状态
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            // 编译着色器失败
            if (compileStatus[0] == 0) {
                Log.e(TAG, "compile shader error: " + GLES20.glGetShaderInfoLog(shaderHandle))
                GLES20.glDeleteShader(shaderHandle)
                shaderHandle = 0
            }
        }
        if (shaderHandle == 0){
            throw RuntimeException("create shader error.")
        }
        return shaderHandle
    }

    /**
     * 创建Program
     * 附件Shader到Program
     * Shader链接到Program
     */
    fun createAndLinkProgram(vertexShaderHandle: Int, fragmentShaderHandle: Int): Int {
        var programHandle = GLES20.glCreateProgram()
        if (programHandle != 0) {
            GLES20.glAttachShader(programHandle, vertexShaderHandle)
            GLES20.glAttachShader(programHandle, fragmentShaderHandle)
            GLES20.glLinkProgram(programHandle)

            // Get the link status.
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)
            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Log.e(TAG, "program link error: " + GLES20.glGetProgramInfoLog(programHandle))
                GLES20.glDeleteProgram(programHandle)
                programHandle = 0
            }
        }
        if (programHandle == 0){
            throw RuntimeException("create program error")
        }
        return programHandle
    }

    init {
        Matrix.setIdentityM(sIdentityMatrix, 0)
    }
}