package com.manu.mediasamples.samples.opengl

import android.view.Surface

/**
 * @Desc:
 * @Author: jzman
 * @Date: 2021/8/5.
 */
interface IRender {
    fun draw()
    fun getSurface():Surface
}