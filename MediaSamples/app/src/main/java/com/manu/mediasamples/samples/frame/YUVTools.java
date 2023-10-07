package com.manu.mediasamples.samples.frame;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.nio.ByteBuffer;

public class YUVTools {

    /******************************* YUV420旋转算法 *******************************/

    // I420或YV12顺时针旋转
    public static void rotateP(byte[] src, byte[] dest, int w, int h, int rotation) {
        switch (rotation) {
            case 0:
                System.arraycopy(src, 0, dest, 0, src.length);
                break;
            case 90:
                rotateP90(src, dest, w, h);
                break;
            case 180:
                rotateP180(src, dest, w, h);
                break;
            case 270:
                rotateP270(src, dest, w, h);
                break;
        }
    }

    // NV21或NV12顺时针旋转
    public static void rotateSP(byte[] src, byte[] dest, int w, int h, int rotation) {
        switch (rotation) {
            case 0:
                System.arraycopy(src, 0, dest, 0, src.length);
                break;
            case 90:
                rotateSP90(src, dest, w, h);
                break;
            case 180:
                rotateSP180(src, dest, w, h);
                break;
            case 270:
                rotateSP270(src, dest, w, h);
                break;
        }
    }
    
    // NV21或NV12顺时针旋转90度
    public static void rotateSP90(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = 0;
        for (int i = 0; i <= w - 1; i++) {
            for (int j = h - 1; j >= 0; j--) {
                dest[k++] = src[j * w + i];
            }
        }

        pos = w * h;
        for (int i = 0; i <= w - 2; i += 2) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w + i];
                dest[k++] = src[pos + j * w + i + 1];
            }
        }
    }

    // NV21或NV12顺时针旋转270度
    public static void rotateSP270(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = 0;
        for (int i = w - 1; i >= 0; i--) {
            for (int j = 0; j <= h - 1; j++) {
                dest[k++] = src[j * w + i];
            }
        }

        pos = w * h;
        for (int i = w - 2; i >= 0; i -= 2) {
            for (int j = 0; j <= h / 2 - 1; j++) {
                dest[k++] = src[pos + j * w + i];
                dest[k++] = src[pos + j * w + i + 1];
            }
        }
    }

    // NV21或NV12顺时针旋转180度
    public static void rotateSP180(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = w * h - 1;
        while (k >= 0) {
            dest[pos++] = src[k--];
        }

        k = src.length - 2;
        while (pos < dest.length) {
            dest[pos++] = src[k];
            dest[pos++] = src[k + 1];
            k -= 2;
        }
    }

    // I420或YV12顺时针旋转90度
    public static void rotateP90(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        //旋转Y
        int k = 0;
        for (int i = 0; i < w; i++) {
            for (int j = h - 1; j >= 0; j--) {
                dest[k++] = src[j * w + i];
            }
        }
        //旋转U
        pos = w * h;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }

        //旋转V
        pos = w * h * 5 / 4;
        for (int i = 0; i < w / 2; i++) {
            for (int j = h / 2 - 1; j >= 0; j--) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }
    }

    // I420或YV12顺时针旋转270度
    public static void rotateP270(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        //旋转Y
        int k = 0;
        for (int i = w - 1; i >= 0; i--) {
            for (int j = 0; j < h; j++) {
                dest[k++] = src[j * w + i];
            }
        }
        //旋转U
        pos = w * h;
        for (int i = w / 2 - 1; i >= 0; i--) {
            for (int j = 0; j < h / 2; j++) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }

        //旋转V
        pos = w * h * 5 / 4;
        for (int i = w / 2 - 1; i >= 0; i--) {
            for (int j = 0; j < h / 2; j++) {
                dest[k++] = src[pos + j * w / 2 + i];
            }
        }
    }

    // I420或YV12顺时针旋转180度
    public static void rotateP180(byte[] src, byte[] dest, int w, int h) {
        int pos = 0;
        int k = w * h - 1;
        while (k >= 0) {
            dest[pos++] = src[k--];
        }

        k = w * h * 5 / 4;
        while (k >= w * h) {
            dest[pos++] = src[k--];
        }

        k = src.length - 1;
        while (pos < dest.length) {
            dest[pos++] = src[k--];
        }
    }

    /******************************* YUV420格式相互转换算法 *******************************/

    // i420 -> nv12, yv12 -> nv21
    public static void pToSP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[pos++] = src[u++];
            dest[pos++] = src[v++];
        }
    }

    // i420 -> nv21, yv12 -> nv12
    public static void pToSPx(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[pos++] = src[v++];
            dest[pos++] = src[u++];
        }
    }

    // nv12 -> i420, nv21 -> yv12
    public static void spToP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[u++] = src[pos++];
            dest[v++] = src[pos++];
        }
    }

    // nv12 -> yv12, nv21 -> i420
    public static void spToPx(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int u = pos;
        int v = pos + (pos >> 2);
        System.arraycopy(src, 0, dest, 0, pos);
        while (pos < src.length) {
            dest[v++] = src[pos++];
            dest[u++] = src[pos++];
        }
    }

    // i420 <-> yv12
    public static void pToP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        int off = pos >> 2;
        System.arraycopy(src, 0, dest, 0, pos);
        System.arraycopy(src, pos, dest, pos + off, off);
        System.arraycopy(src, pos + off, dest, pos, off);
    }

    // nv12 <-> nv21
    public static void spToSP(byte[] src, byte[] dest, int w, int h) {
        int pos = w * h;
        System.arraycopy(src, 0, dest, 0, pos);
        for (; pos < src.length; pos += 2) {
            dest[pos] = src[pos + 1];
            dest[pos + 1] = src[pos];
        }
    }


    /******************************* YUV420转换Bitmap算法 *******************************/

    // 此方法虽然是官方的，但是耗时是下面方法的两倍
//    public static Bitmap nv21ToBitmap(byte[] data, int w, int h) {
//        final YuvImage image = new YuvImage(data, ImageFormat.NV21, w, h, null);
//        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
//        if (image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
//            byte[] tmp = os.toByteArray();
//            return BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
//        }
//        return null;
//    }

    public static Bitmap nv12ToBitmap(byte[] data, int w, int h) {
        return spToBitmap(data, w, h, 0, 1);
    }

    public static Bitmap nv21ToBitmap(byte[] data, int w, int h) {
        return spToBitmap(data, w, h, 1, 0);
    }

    private static Bitmap spToBitmap(byte[] data, int w, int h, int uOff, int vOff) {
        int plane = w * h;
        int[] colors = new int[plane];
        int yPos = 0, uvPos = plane;
        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                // YUV byte to RGB int
                final int y1 = data[yPos] & 0xff;
                final int u = (data[uvPos + uOff] & 0xff) - 128;
                final int v = (data[uvPos + vOff] & 0xff) - 128;
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);
                colors[yPos] = ((r << 6) & 0xff0000) |
                        ((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);

                if((yPos++ & 1) == 1) uvPos += 2;
            }
            if((j & 1) == 0) uvPos -= w;
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.RGB_565);
    }

    public static Bitmap i420ToBitmap(byte[] data, int w, int h) {
        return pToBitmap(data, w, h, true);
    }

    public static Bitmap yv12ToBitmap(byte[] data, int w, int h) {
        return pToBitmap(data, w, h, false);
    }

    private static Bitmap pToBitmap(byte[] data, int w, int h, boolean uv) {
        int plane = w * h;
        int[] colors = new int[plane];
        int off = plane >> 2;
        int yPos = 0, uPos = plane + (uv ? 0 : off), vPos = plane + (uv ? off : 0);
        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                // YUV byte to RGB int
                final int y1 = data[yPos] & 0xff;
                final int u = (data[uPos] & 0xff) - 128;
                final int v = (data[vPos] & 0xff) - 128;
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);
                colors[yPos] = ((r << 6) & 0xff0000) |
                        ((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);

                if((yPos++ & 1) == 1) {
                    uPos++;
                    vPos++;
                }
            }
            if((j & 1) == 0) {
                uPos -= (w >> 1);
                vPos -= (w >> 1);
            }
        }
        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.RGB_565);
    }

    public static int[] planesToColors(Image.Plane[] planes, int height) {
        ByteBuffer yPlane = planes[0].getBuffer();
        ByteBuffer uPlane = planes[1].getBuffer();
        ByteBuffer vPlane = planes[2].getBuffer();

        int bufferIndex = 0;
        final int total = yPlane.capacity();
        final int uvCapacity = uPlane.capacity();
        final int width = planes[0].getRowStride();

        int[] rgbBuffer = new int[width * height];

        int yPos = 0;
        for (int i = 0; i < height; i++) {
            int uvPos = (i >> 1) * width;

            for (int j = 0; j < width; j++) {
                if (uvPos >= uvCapacity - 1)
                    break;
                if (yPos >= total)
                    break;

                final int y1 = yPlane.get(yPos++) & 0xff;

            /*
              The ordering of the u (Cb) and v (Cr) bytes inside the planes is a
              bit strange. The _first_ byte of the u-plane and the _second_ byte
              of the v-plane build the u/v pair and belong to the first two pixels
              (y-bytes), thus usual YUV 420 behavior. What the Android devs did
              here (IMHO): just copy the interleaved NV21 U/V data to two planes
              but keep the offset of the interleaving.
             */
                final int u = (uPlane.get(uvPos) & 0xff) - 128;
                final int v = (vPlane.get(uvPos) & 0xff) - 128;
                if ((j & 1) == 1) {
                    uvPos += 2;
                }

                // This is the integer variant to convert YCbCr to RGB, NTSC values.
                // formulae found at
                // https://software.intel.com/en-us/android/articles/trusted-tools-in-the-new-android-world-optimization-techniques-from-intel-sse-intrinsics-to
                // and on StackOverflow etc.
                final int y1192 = 1192 * y1;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                r = (r < 0) ? 0 : ((r > 262143) ? 262143 : r);
                g = (g < 0) ? 0 : ((g > 262143) ? 262143 : g);
                b = (b < 0) ? 0 : ((b > 262143) ? 262143 : b);

                rgbBuffer[bufferIndex++] = ((r << 6) & 0xff0000) |
                        ((g >> 2) & 0xff00) |
                        ((b >> 10) & 0xff);
            }
        }
        return rgbBuffer;
    }

    /**
     * 从ImageReader中获取byte[]数据
     */
    public static byte[] getBytesFromImageReader1(ImageReader imageReader) {
        try (Image image = imageReader.acquireNextImage()) {
            final Image.Plane[] planes = image.getPlanes();
            ByteBuffer b0 = planes[0].getBuffer();
            ByteBuffer b1 = planes[1].getBuffer();
            ByteBuffer b2 = planes[2].getBuffer();
            int y = b0.remaining(), u = y >> 2, v = u;
            byte[] bytes = new byte[y + u + v];
            if(b1.remaining() > u) { // y420sp
                b0.get(bytes, 0, b0.remaining());
                b1.get(bytes, y, b1.remaining()); // uv
            } else { // y420p
                b0.get(bytes, 0, b0.remaining());
                b1.get(bytes, y, b1.remaining()); // u
                b2.get(bytes, y + u, b2.remaining()); // v
            }
            image.close();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getBytesFromImageReader(ImageReader imageReader) {
        Image image = imageReader.acquireNextImage();

        // 获取YUV数据
        ByteBuffer byteBufferY = image.getPlanes()[0].getBuffer();
        int byteBufferYSize = byteBufferY.remaining();
        ByteBuffer byteBufferU = image.getPlanes()[1].getBuffer();
        int byteBufferUSize = byteBufferU.remaining();
        ByteBuffer byteBufferV = image.getPlanes()[2].getBuffer();
        int byteBufferVSize = byteBufferV.remaining();

        // 填充YUV数据
        byte[] mYUVByteArray = new byte[byteBufferYSize + byteBufferUSize + byteBufferVSize];
        byteBufferY.get(mYUVByteArray, 0, byteBufferYSize);
        byteBufferU.get(mYUVByteArray, byteBufferYSize, byteBufferUSize);
        byteBufferV.get(mYUVByteArray, byteBufferYSize + byteBufferUSize, byteBufferVSize);

        image.close();
        Log.i("jzman", "getBytesFromImageReader > size: "+mYUVByteArray.length);
        return mYUVByteArray;
    }


}
