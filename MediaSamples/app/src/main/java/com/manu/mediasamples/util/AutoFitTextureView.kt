package com.manu.mediasamples.util

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView

/**
 * @Desc: AutoFitTexture
 * @Author: jzman
 */
class AutoFitTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr) {

    companion object{
        private val TAG = AutoFitTextureView::class.java.simpleName
    }

    private var mRatioWidth = 0
    private var mRatioHeight = 0

    fun setAspectRatio(width:Int, height:Int){
        require(width > 0 && height > 0){"width and height cannot be negative or zero."}
        this.mRatioWidth = width
        this.mRatioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        Log.d(TAG, "Measured dimensions origin size: $width x $height")
        val newWidth: Int
        val newHeight: Int
        if (mRatioWidth == 0 || mRatioHeight == 0){
            newWidth = width
            newHeight = height
        }else{
            if (width < height * mRatioWidth / mRatioHeight) {
                newWidth = width
                newHeight = width * mRatioHeight / mRatioWidth
            } else {
                newWidth = height * mRatioWidth / mRatioHeight
                newHeight = height
            }
            setMeasuredDimension(newWidth, newHeight)
        }
        Log.d(TAG, "Measured dimensions set: $newWidth x $newHeight")
    }

}