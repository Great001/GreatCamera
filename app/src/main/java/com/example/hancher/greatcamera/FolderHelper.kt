package com.example.hancher.greatcamera

import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat

/**
 * 文件工具类
 * Created by liaohaicong on 2019/4/23.
 */
object FolderHelper {

    private val APP_DIR = "SuperCamera"
    private val PIC_DIR = "DCIM"

    fun getPicSaveDir(): String {
        val folder: File
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            folder = File(Environment.getExternalStorageDirectory().absolutePath + File.separator + APP_DIR)
            if (!folder.exists()) {
                //创建非应用私有的sdcard外部文件，6.0之后必须动态申请存储权限,否则文件创建失败
                val isSuccess = folder.mkdirs()
                Loger.d("sd卡创建SuperCamera文件目录是否成功：$isSuccess")
            }
        } else {
            folder = File(GreatApp.getAppContext().filesDir.absolutePath + File.separator + PIC_DIR)
            if (!folder.exists()) {
                folder.mkdir()
            }
        }
        return folder.absolutePath
    }

    fun getCurPicSavePath(): String {
        return getPicSaveDir() + File.separator + formatTime(System.currentTimeMillis()) + ".jpg"
    }

    /**
     * 格式化时间
     */
    private fun formatTime(time: Long): String {
        val simpleFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        return simpleFormat.format(time)
    }
}