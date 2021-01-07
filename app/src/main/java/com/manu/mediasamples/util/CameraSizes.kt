
package com.manu.mediasamples.util

import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.Display
import kotlin.math.max
import kotlin.math.min

/**
 * Size辅助类
 */
class SmartSize(width: Int, height: Int) {
    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}

/** 图片和视频的标准高清尺寸 */
val SIZE_1080P: SmartSize =
    SmartSize(1920, 1080)

/**
 * 获得给定Display对应屏幕真实尺寸的SmartSize
 */
fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

/**
 * 获取可用的最大预览尺寸
 */
fun <T> getPreviewOutputSize(
        display: Display,
        characteristics: CameraCharacteristics,
        targetClass: Class<T>,
        format: Int? = null
): Size {

    val screenSize = getDisplaySmartSize(display)
    val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
    val maxSize = if (hdScreen) SIZE_1080P else screenSize

    // 如果提供图像格式则由具体格式决定预览大小，否则则由targetClass决定
    val config = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
    // 检查提供的target和format是否支持
    if (format == null) {
        if (!StreamConfigurationMap.isOutputSupportedFor(targetClass)) error("$targetClass not support.")
    }else{
        if (!config.isOutputSupportedFor(format)) error("$format not support.")
    }
    val allSizes = if (format == null)
        config.getOutputSizes(targetClass) else config.getOutputSizes(format)

    // 根据Size从大到小排序
    val validSizes = allSizes
            .sortedWith(compareBy { it.height * it.width })
            .map { SmartSize(it.width, it.height) }
            .reversed()

    return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
}