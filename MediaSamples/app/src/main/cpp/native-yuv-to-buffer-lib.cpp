//
// Created by awk-asst on 11.04.19.
//

#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <asm/fcntl.h>
#include <fcntl.h>
#include <android/native_window_jni.h>
#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>

#define TAG "CamCapture"

uint8_t *buf;
uint8_t *bbuf_yIn;
uint8_t *bbuf_uIn;
uint8_t *bbuf_vIn;

bool SPtoI420(const uint8_t *src, uint8_t *dst, int width, int height, bool isNV21)
{
    if (!src || !dst) {
        return false;
    }

    unsigned int YSize = width * height;
    unsigned int UVSize = (YSize>>1);

    // NV21: Y..Y + VUV...U
    const uint8_t *pSrcY = src;
    const uint8_t *pSrcUV = src + YSize;

    // I420: Y..Y + U.U + V.V
    uint8_t *pDstY = dst;
    uint8_t *pDstU = dst + YSize;
    uint8_t *pDstV = dst + YSize + (UVSize>>1);

    // copy Y
    memcpy(pDstY, pSrcY, YSize);

    // copy U and V
    for (int k=0; k < (UVSize>>1); k++) {
        if(isNV21) {
            pDstV[k] = pSrcUV[k * 2];     // copy V
            pDstU[k] = pSrcUV[k * 2 + 1];   // copy U
        }else{
            pDstU[k] = pSrcUV[k * 2];     // copy V
            pDstV[k] = pSrcUV[k * 2 + 1];   // copy U
        }
    }

    return true;
}

bool NV21toNV12(const uint8_t *src, uint8_t *dst, int width, int height)
{
    if (!src || !dst) {
        return false;
    }

    unsigned int YSize = width * height;
    unsigned int UVSize = (YSize>>1);

    // NV21: Y..Y + VUV...U
    const uint8_t *pSrcY = src;
    const uint8_t *pSrcUV = src + YSize;

    // NV12: Y..Y + UVU...V
    uint8_t *pDstY = dst;
    uint8_t *pDstUV = dst + YSize;

    // copy Y
    memcpy(pDstY, pSrcY, YSize);

    // copy U and V
    for (int k=0; k < (UVSize>>1); k++) {
            pDstUV[k * 2 + 1] = pSrcUV[k * 2];     // copy V
            pDstUV[k * 2] = pSrcUV[k * 2 + 1];   // copy U
    }

    return true;
}


/*-----------------------------------------------------------------------*/
/*-----------------------------------------------------------------------*/
/*------------------------- send frame planes ---------------------------*/
/*-----------------------------------------------------------------------*/
/*-----------------------------------------------------------------------*/
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_manu_mediasamples_frame_IMediaNative_yuvToBuffer(JNIEnv *env, jobject instance,
                                                              jobject yPlane, jobject uPlane, jobject vPlane,
                                                              jint yPixelStride, jint yRowStride,
                                                              jint uPixelStride, jint uRowStride,
                                                              jint vPixelStride, jint vRowStride,
                                                              jint imgWidth, jint imgHeight) {

    bbuf_yIn = static_cast<uint8_t *>(env->GetDirectBufferAddress(yPlane));
    bbuf_uIn = static_cast<uint8_t *>(env->GetDirectBufferAddress(uPlane));
    bbuf_vIn = static_cast<uint8_t *>(env->GetDirectBufferAddress(vPlane));

    buf = (uint8_t *) malloc(sizeof(uint8_t) * imgWidth * imgHeight +
                             2 * (imgWidth + 1) / 2 * (imgHeight + 1) / 2);

//    __android_log_print(ANDROID_LOG_INFO, "YUVTOBUFFER", "yPixelStride: %d, yRowStride: %d", yPixelStride, yRowStride);
//    __android_log_print(ANDROID_LOG_INFO, "YUVTOBUFFER", "uPixelStride: %d, uRowStride: %d", uPixelStride, uRowStride);
//    __android_log_print(ANDROID_LOG_INFO, "YUVTOBUFFER", "vPixelStride: %d, vRowStride: %d", vPixelStride, vRowStride);
//    __android_log_print(ANDROID_LOG_INFO, "YUVTOBUFFER", "bbuf_yIn: %p, bbuf_uIn: %p, bbuf_vIn: %p", bbuf_yIn, bbuf_uIn, bbuf_vIn);
//    __android_log_print(ANDROID_LOG_INFO, "YUVTOBUFFER", "imgWidth: %d, imgHeight: %d", imgWidth, imgHeight);

    bool isNV21;
    if (yPixelStride == 1) {
        // All pixels in a row are contiguous; copy one line at a time.
        for (int y = 0; y < imgHeight; y++)
            memcpy(buf + y * imgWidth, bbuf_yIn + y * yRowStride,
                   static_cast<size_t>(imgWidth));
    } else {
        // Highly improbable, but not disallowed by the API. In this case
        // individual pixels aren't stored consecutively but sparsely with
        // other data inbetween each pixel.
        for (int y = 0; y < imgHeight; y++)
            for (int x = 0; x < imgWidth; x++)
                buf[y * imgWidth + x] = bbuf_yIn[y * yRowStride + x * yPixelStride];
    }

    uint8_t *chromaBuf = &buf[imgWidth * imgHeight];
    int chromaBufStride = 2 * ((imgWidth + 1) / 2);



    if (uPixelStride == 2 && vPixelStride == 2 &&
        uRowStride == vRowStride && bbuf_uIn == bbuf_vIn + 1) {


        isNV21 = true;
        // The actual cb/cr planes happened to be laid out in
        // exact NV21 form in memory; copy them as is
        for (int y = 0; y < (imgHeight + 1) / 2; y++)
            memcpy(chromaBuf + y * chromaBufStride, bbuf_vIn + y * vRowStride,
                   static_cast<size_t>(chromaBufStride));


    } else if (vPixelStride == 2 && uPixelStride == 2 &&
               uRowStride == vRowStride && bbuf_vIn == bbuf_uIn + 1) {


        isNV21 = false;
        // The cb/cr planes happened to be laid out in exact NV12 form
        // in memory; if the destination API can use NV12 in addition to
        // NV21 do something similar as above, but using cbPtr instead of crPtr.
        // If not, remove this clause and use the generic code below.
    }
    else {
        isNV21 = true;
        if (vPixelStride == 1 && uPixelStride == 1) {
            // Continuous cb/cr planes; the input data was I420/YV12 or similar;
            // copy it into NV21 form
            for (int y = 0; y < (imgHeight + 1) / 2; y++) {
                for (int x = 0; x < (imgWidth + 1) / 2; x++) {
                    chromaBuf[y * chromaBufStride + 2 * x + 0] = bbuf_vIn[y * vRowStride + x];
                    chromaBuf[y * chromaBufStride + 2 * x + 1] = bbuf_uIn[y * uRowStride + x];
                }
            }
        } else {
            // Generic data copying into NV21
            for (int y = 0; y < (imgHeight + 1) / 2; y++) {
                for (int x = 0; x < (imgWidth + 1) / 2; x++) {
                    chromaBuf[y * chromaBufStride + 2 * x + 0] = bbuf_vIn[y * vRowStride +
                                                                          x * uPixelStride];
                    chromaBuf[y * chromaBufStride + 2 * x + 1] = bbuf_uIn[y * uRowStride +
                                                                          x * vPixelStride];
                }
            }
        }
    }

    if (isNV21) {
        __android_log_print(ANDROID_LOG_INFO, "YUVTOBUFFER", "isNV21");
    }
    else {
        __android_log_print(ANDROID_LOG_INFO, "YUVTOBUFFER", "isNV12");
    }

//    uint8_t *I420Buff = (uint8_t *) malloc(sizeof(uint8_t) * imgWidth * imgHeight +
//                                           2 * (imgWidth + 1) / 2 * (imgHeight + 1) / 2);
    uint8_t *NV12Buff = (uint8_t *) malloc(sizeof(uint8_t) * imgWidth * imgHeight +
                                           2 * (imgWidth + 1) / 2 * (imgHeight + 1) / 2);
//    SPtoI420(buf,I420Buff,imgWidth,imgHeight,isNV21);
    if (isNV21) {
        NV21toNV12(buf, NV12Buff, imgWidth, imgHeight);
    }
    else {
        NV12Buff = buf;
    }

    jbyteArray ret = env->NewByteArray(imgWidth * imgHeight *
                                       3/2);
    env->SetByteArrayRegion (ret, 0, imgWidth * imgHeight *
                                     3/2, (jbyte*)NV12Buff);
    free(buf);
    free(NV12Buff);
//    free (I420Buff);
    return ret;
}
