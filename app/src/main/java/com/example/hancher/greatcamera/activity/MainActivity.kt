package com.example.hancher.greatcamera.activity

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
import com.example.hancher.greatcamera.R
import com.example.hancher.greatcamera.utils.Loger
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    companion object {

        private val RC_IMAGE_CAPTURE = 1111
        private val RC_VIDEO_CAPTURE = 1112
        private val RC_IMAGE_CROP = 1113
        private val RC_CAMERA = 1114

        private val ACTION_IMAGE_CROP = "com.android.camera.action.CROP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkToRequestPermissions()
        tv_goto_image_capture.setOnClickListener {
            gotoImageCapture()
        }

        tv_goto_video_capture.setOnClickListener {
            gotoVideoCapture()
        }

        tv_goto_imagee_crop.setOnClickListener {
            gotoImageCrop()
        }

        tv_goto_camera.setOnClickListener {
            gotoCamera(false)
        }

        tv_goto_camera_encode.setOnClickListener {
            gotoCamera(true)
        }

        tv_goto_video_player.setOnClickListener {
            gotoVideoPlayer()
        }
    }

    private fun gotoCamera(userEncoder:Boolean) {
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra(CameraActivity.KEY_USE_ENCODER,userEncoder)
        startActivityForResult(intent, RC_CAMERA)
    }

    private fun gotoVideoPlayer() {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        startActivity(intent)
    }

    /**
     * 系统相机拍照
     */
    private fun gotoImageCapture() {
        val outputUri = getImageCaptureOutputFileUri()
        Loger.d("拍照文件保存路径= $outputUri")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //若未传入保存路径，拍的照片不会保存，只会在onActivityResult的data返回一个缩略图bitmap
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        startActivityForResult(intent, RC_IMAGE_CAPTURE)
    }

    private fun gotoVideoCapture() {
        val outputUri = getVideoCaptureOutputFileUri()
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        startActivityForResult(intent, RC_VIDEO_CAPTURE)
    }

    private fun gotoImageCrop() {
        val cropUri = getImageCropFileUri()
        val outputUri = getImageCaptureOutputFileUri()
        val intent = Intent(ACTION_IMAGE_CROP)

        //一定要重新授予权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        intent.setDataAndType(cropUri, "image/*")
        //千万不能分别设置啊，会各自清楚的，但是data和type少一个都不行，所以只能调用setDataAndType
//        intent.data = cropUri
//        intent.type = "image/*"
        intent.putExtra("crop", "true")
        intent.putExtra("scale", true)
        intent.putExtra("aspectX", 1)  //裁剪比例
        intent.putExtra("aspectY", 1)
        //针对7.0以上的系统由于使用FileProvider,所以外部该FileProvider应该grantUriPermission,否则抛出异常
        // java.lang.SecurityException: Permission Denial: writing android.support.v4.content.FileProvider uri content://com.example.hancher.greatcamera.myprovider/ep_pic/20190422_214829.jpg
        // from pid=14060, uid=10067 requires the provider be exported, or grantUriPermission()
        //所以7.0以上的手机还是不要指定输出路径，直接使用默认路径，在onActivityResult会返回uri
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)  //裁剪后图片保存路径
        }
        intent.putExtra("outputX", 540)  //裁剪后图片宽
        intent.putExtra("outputY", 540)  //裁剪后图片高
        intent.putExtra("return-data", false)
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())  //裁剪后图片格式
        startActivityForResult(intent, RC_IMAGE_CROP)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RC_IMAGE_CAPTURE) {
                Loger.d("调用系统相机拍照完成回调")
                if (data == null) {
                    Toast.makeText(this, "拍照完成", Toast.LENGTH_SHORT).show()
                    Loger.d("照片已经拍好了存放在预设路径中，没有返回数据了")
                    return
                }
                val uri = data.data
                if (uri != null) {
                    Loger.d("拍照返回的Uri = $uri")
                }
                val bitmap = data.getParcelableExtra<Bitmap>("data")
                if (bitmap == null) {
                    Loger.d("没有返回data缩略图数据")
                }
                Loger.d("调用系统相机拍照完成，生成缩略图Bitmap: width = ${bitmap.width} height = ${bitmap.height}")
            } else if (requestCode == RC_VIDEO_CAPTURE) {
                Loger.d("调用系统相机录像回调")
                Toast.makeText(this, "录像完成", Toast.LENGTH_SHORT).show()
                if (data == null) {
                    return
                }
                val uri = data.data
                if (uri != null) {
                    Loger.d("录像返回Uri = $uri")
                }
            } else if (requestCode == RC_IMAGE_CROP) {
                Loger.d("调用系统相机裁剪回调")
                Toast.makeText(this, "裁剪完成", Toast.LENGTH_SHORT).show()
                if (data == null) {
                    return
                }
                val uri = data.data
                if (uri != null) {
                    Loger.d("裁剪返回Uri = $uri")
                }
            } else if (requestCode == RC_CAMERA) {
                Loger.d("拍摄页返回")
            }
        }
    }

    private fun getImageCaptureOutputFileUri(): Uri {
        // 7.0之后不能直接向外传递file类型的Uri,只能传递content类型的Uri，file类型的可以通过FileProvider转成Content Uri
        // 否则，会报  android.os.FileUriExposedException: file:///storage/emulated/0/SuperCamera/20190422_185649.jpg exposed beyond app through ClipData.Item.getUri()
        val outputFile = File(createSaveFolder().path + File.separator + formatTime(System.currentTimeMillis()) + ".jpg")
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

    private fun getVideoCaptureOutputFileUri(): Uri {
        val outputFile = File(createSaveFolder().path + File.separator + formatTime(System.currentTimeMillis()) + ".mp4")
        if (!outputFile.exists()) {
            outputFile.createNewFile()
        }
        val outputUri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Uri.fromFile(outputFile)
        } else {
            FileProvider.getUriForFile(this, "com.example.hancher.greatcamera.myprovider", outputFile)
        }
        grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return outputUri
    }

    private fun getImageCropFileUri(): Uri {
        val cropFile = File(createSaveFolder().path + File.separator + "test.jpg")
        if (!cropFile.exists()) {
            cropFile.createNewFile()
        }
        val cropUri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Uri.fromFile(cropFile)
        } else {
            FileProvider.getUriForFile(this, "com.example.hancher.greatcamera.myprovider", cropFile)
        }
        grantUriPermission(packageName, cropUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        grantUriPermission(packageName, cropUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return cropUri
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
