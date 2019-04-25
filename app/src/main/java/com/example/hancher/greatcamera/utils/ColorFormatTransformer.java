package com.example.hancher.greatcamera.utils;

/**
 * NV21转NV12
 * Created by liaohaicong on 2019/4/24.
 */

public class ColorFormatTransformer {
    public static void NV21ToNV12(byte[] source21, byte[] result12, int width, int height) {
        if (source21 == null || result12 == null) return;
        int length = width * height;
        int i,j;
        System.arraycopy(source21, 0, result12, 0, length);
        for (i = 0; i < length; i++) {
            result12[i] = source21[i];
        }
        for (j = 0; j < length / 2; j += 2) {
            result12[length + j - 1] = source21[j + length];
        }
        for (j = 0; j < length / 2; j += 2) {
            result12[length + j] = source21[j + length - 1];
        }
    }
}
