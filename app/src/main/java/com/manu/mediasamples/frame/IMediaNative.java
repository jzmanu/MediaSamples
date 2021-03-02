package com.manu.mediasamples.frame;

import java.nio.ByteBuffer;

class IMediaNative {
    public native byte[] yuvToBuffer(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yPixelStride, int yRowStride,
                                            int uPixelStride, int uRowStride, int vPixelStride, int vRowStride, int imgWidth, int imgHeight);
}
