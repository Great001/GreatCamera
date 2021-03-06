package com.example.hancher.greatcamera.utils

import android.os.Environment
import com.example.hancher.greatcamera.GreatApp
import java.io.File
import java.text.SimpleDateFormat

/**
 * 文件工具类
 * Created by liaohaicong on 2019/4/23.
 */
object FolderHelper {

    private val APP_DIR = "SuperCamera"
    private val PIC_DIR = "Pictures"
    private val VIDEO_DIR = "Videos"

    fun getExternalAppDir(): File {
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
        return folder
    }

    fun getPicSaveDir(): String {
        return getExternalAppDir().absolutePath + File.separator + PIC_DIR
    }

    fun getVideoSaveDir(): String {
        return getExternalAppDir().absolutePath + File.separator + VIDEO_DIR
    }

    fun getCurPicSavePath(): String {
        return getPicSaveDir() + File.separator + formatTime(System.currentTimeMillis()) + ".jpg"
    }


    fun getCurVideoSavePath(): String {
        return getVideoSaveDir() + File.separator + formatTime(System.currentTimeMillis()) + ".MP4"
    }

    /**
     * 格式化时间
     */
    private fun formatTime(time: Long): String {
        val simpleFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        return simpleFormat.format(time)
    }
}