package com.manu.mediasamples.util

import android.util.Log
import com.manu.mediasamples.BuildConfig

/**
 * @Desc:日志工具类
 * @Author: jzman
 * @Date: 2021/4/24 11:57.
 */
object L {
    private const val TAG = "MS"
    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "$tag > $msg")
        }
    }

    fun e(tag: String, msg: String?,tr:Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "$tag > $msg",tr)
        }
    }
}