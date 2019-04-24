package com.example.hancher.greatcamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.SurfaceHolder
import android.view.View
import kotlinx.android.synthetic.main.activity_camera.*

/**
 * Created by liaohaicong on 2019/4/23.
 */
class CameraActivity : FragmentActivity(), SurfaceHolder.Callback {

    companion object {
        private val RC_CAMERA = 1120
        private val RC_AUDIO = 1121
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //设置全屏模式
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_camera)

        switchCameraView.setOnClickListener {
            picShowView.visibility = View.GONE
            SuperCamera.instance().toggleCamera(surfaceView)
        }

        shotButton.setOnClickListener {
            SuperCamera.instance().takePicture(Camera.PictureCallback { data, camera ->
                Loger.d("拍照完成")
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    picShowView.visibility = View.VISIBLE
                    saveView.visibility = View.VISIBLE
                    picShowView.setImageBitmap(bitmap)
                    BitmapUtil.saveTakenBitmap(bitmap)
                    SuperCamera.instance().closeCamera()
                }
            })
        }

        recordBtn.setOnClickListener {
            if (SuperCamera.instance().isCameraAvailable()) {
                if (!SuperCamera.instance().isRecording()) {
                    picShowView.visibility = View.GONE
                    recordBtn.text = "..."
                    SuperCamera.instance().recordVideoWithEncoder()
                } else {
                    SuperCamera.instance().stopRecordVideoWithEncoder()
                    recordBtn.text = "record"
                }
            }
        }

        saveView.setOnClickListener {
            picShowView.visibility = View.GONE
            saveView.visibility = View.GONE
            SuperCamera.instance().openCamera(false, true, surfaceView)
        }

        surfaceView.holder.addCallback(this)
    }


    override fun onDestroy() {
        super.onDestroy()
        SuperCamera.instance().closeCamera()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Loger.d("SurfaceCreated")
        if (checkToRequestCamerePermission() && checkToRequestMicPermission()) {
            SuperCamera.instance().openCamera(false, true, surfaceView)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Loger.d("SurfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Loger.d("SurfaceDestroyed")
        SuperCamera.instance().closeCamera()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (RC_CAMERA == requestCode) {
            permissions.forEachIndexed { index, s ->
                if (permissions[index] == (Manifest.permission.CAMERA) && grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    Loger.d("相机权限申请成功")
                    SuperCamera.instance().openCamera(false, true, surfaceView)
                    return@forEachIndexed
                }
            }
        }
    }

    /**
     * 动态权限相机申请
     */
    private fun checkToRequestCamerePermission(): Boolean {
        //要想在SD中创建文件，必须动态申请文件读写读写权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), RC_CAMERA)
            return false
        }
        return true
    }

    /**
     * 动态权限相机申请
     */
    private fun checkToRequestMicPermission(): Boolean {
        //要想在SD中创建文件，必须动态申请文件读写读写权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), RC_CAMERA)
            return false
        }
        return true
    }
}