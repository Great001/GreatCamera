package com.example.hancher.greatcamera.utils

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/**
 * Created by liaohaicong on 2019/4/23.
 */
object BitmapUtil {

    fun saveTakenBitmap(bitmap: Bitmap) {
        val savePath = FolderHelper.getCurPicSavePath()
        saveBitmap(bitmap, savePath)
    }

    fun saveBitmap(bitmap: Bitmap, savePath: String) {
        if (bitmap == null || bitmap.isRecycled) {
            return
        }
        val saveFile = File(savePath)
        val parentFile = saveFile.parentFile
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (saveFile.exists()) {
            saveFile.delete()
        }
        saveFile.createNewFile()
        val outputStream = FileOutputStream(saveFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    }

}
