package com.manu.mediasamples.util

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.lang.RuntimeException
import javax.microedition.khronos.opengles.GL10

/**
 * @Desc: TextureHelper
 * @Author: jzman
 * @Date: 2021/8/5 11:17.
 */
object TextureHelper {
    const val TAG = "TextureHelper"

    /**
     * 创建OES纹理
     * YUV格式到RGB的自动转化
     */
    fun createOESTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        if (tex[0] == 0) {
            throw RuntimeException("create OES texture failed, ${Thread.currentThread().name}")
        }

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE.toFloat())

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        Log.d(TAG, "createOESTexture: texture id ${tex[0]}")
        return tex[0]
    }
}