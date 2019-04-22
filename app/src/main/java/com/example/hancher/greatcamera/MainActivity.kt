package com.example.hancher.greatcamera

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    companion object {

        private val RC_IMAGE_CAPTURE = 1111
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkToRequestPermissions()
        tv_goto_image_capture.setOnClickListener {
            gotoImageCapture()
        }
    }

    private fun gotoImageCapture() {
        val outputUri = getOutputFileUri()
        Loger.d("拍照文件保存路径= $outputUri")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //若未传入保存路径，拍的照片不会保存，只会在onActivityResult的data返回一个缩略图bitmap
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        startActivityForResult(intent, RC_IMAGE_CAPTURE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == RC_IMAGE_CAPTURE) {
            Loger.d("调用系统相机拍照完成回调")
            if (data == null) {
                Toast.makeText(this, "拍照完成", Toast.LENGTH_SHORT)
                Loger.d("照片已经拍好了存放在预设路径中，没有返回数据了")
                return
            }
            val bitmap = data.getParcelableExtra<Bitmap>("data")
            if (bitmap == null) {
                Loger.d("没有返回data缩略图数据")
            }
            Loger.d("调用系统相机拍照完成，生成缩略图Bitmap: width = ${bitmap.width} height = ${bitmap.height}")
        }
    }

    private fun getOutputFileUri(): Uri {
        // 7.0之后不能直接向外传递file类型的Uri,只能传递content类型的Uri，file类型的可以通过FileProvider转成Content Uri
        // 否则，会报  android.os.FileUriExposedException: file:///storage/emulated/0/SuperCamera/20190422_185649.jpg exposed beyond app through ClipData.Item.getUri()
        val outputFile = File(createSaveFolder().path + File.separator + formatTime(System.currentTimeMillis()) + ".jpg")
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }
        val outputUri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Uri.fromFile(outputFile)
        } else {
            FileProvider.getUriForFile(this, "com.example.hancher.greatcamera.myprovider", outputFile)
        }
        //content://com.example.hancher.greatcamera.myprovider/ep_pic/20190422_194526.jpg
        //转换成Uri最好添加权限
        grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return outputUri
    }

    private fun createSaveFolder(): File {
        val folder: File
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            folder = File(Environment.getExternalStorageDirectory().absolutePath + File.separator + "SuperCamera")
            if (!folder.exists()) {
                //创建非应用私有的sdcard外部文件，6.0之后必须动态申请存储权限,否则文件创建失败
                val isSuccess = folder.mkdirs()
                Loger.d("sd卡创建SuperCamera文件目录是否成功：$isSuccess")
            }
        } else {
            //4.4之后，sdcard应用私有的目录，无需再申请存储权限
            //storage/emulated/0/Android/data/com.example.hancher.greatcamera/files/Pictures
            folder = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
        Loger.d("保存文件创建成功，文件路径=${folder.absolutePath}")
        return folder
    }

    /**
     * 格式化时间
     */
    private fun formatTime(time: Long): String {
        val simpleFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
        return simpleFormat.format(time)
    }


    /**
     * 动态权限申请
     */
    private fun checkToRequestPermissions() {
        //要想在SD中创建文件，必须动态申请文件读写读写权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }
}
