package com.example.hancher.greatcamera

import android.hardware.Camera
import android.view.SurfaceView

/**
 * 相机
 * Created by liaohaicong on 2019/4/23.
 */
class SuperCamera private constructor() {

    companion object {
        @Volatile
        private var instance: SuperCamera? = null

        fun instance(): SuperCamera {
            return instance ?: synchronized(this) {
                instance ?: SuperCamera().also { instance = it }
            }
        }
    }

    init {
        printCameraInfo()
    }

    private var camera: Camera? = null
    private var isFront: Boolean = false
    private var cameraOrientation: Int = 0

    private var preview: SurfaceView? = null

    fun openCamera(isFront: Boolean, surfaceView: SurfaceView) {
        this.isFront = isFront
        preview = surfaceView
        val facing = if (isFront) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK
        //可能会阻塞，最好放在异步线程中
        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(facing, cameraInfo)
        cameraOrientation = cameraInfo.orientation
        camera = Camera.open(facing)
        val parameters = camera!!.parameters
        printCameraParameters(parameters)
        val previewSize = parameters.previewSize
        parameters.setPictureSize(previewSize.width, previewSize.height)
        if (isFront) {
            parameters.setRotation(cameraOrientation)
        } else {
            parameters.setRotation(cameraOrientation)
        }

        camera?.parameters = parameters
        if (isFront) {
            camera?.setDisplayOrientation(360 - cameraOrientation)
        } else {
            camera?.setDisplayOrientation(cameraOrientation)
        }
        camera?.setPreviewDisplay(surfaceView.holder)
        camera?.startPreview()
    }

    fun toggleCamera() {
        if (camera != null) {
            closeCamera()
            openCamera(!isFront, preview!!)
        }
    }

    fun takePicture(pictureCallback: Camera.PictureCallback) {
        if (camera != null) {
            camera?.takePicture(Camera.ShutterCallback { }, null, pictureCallback)
        }
    }

    fun closeCamera() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun printCameraInfo() {
        //获取摄像头个数,对应的cameraId在0-（cameraNum-1）
        val cameraNum = Camera.getNumberOfCameras()
        for (i in 0 until cameraNum) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(i, cameraInfo)
            Loger.d("摄像头信息： facing=${cameraInfo.facing}，orientation=${cameraInfo.orientation}, ")
        }
    }

    private fun printCameraParameters(parameters: Camera.Parameters) {
        val previewSize = parameters.previewSize
        Loger.d("PreviewSize: width = ${previewSize.width} ,height = ${previewSize.height}")
        val pictureSize = parameters.pictureSize
        Loger.d("PictureSize: width = ${pictureSize.width} ,height = ${pictureSize.height}")
        val previewFormat = parameters.previewFormat
        Loger.d("PreviewFormat: $previewFormat")
        val pictureFormat = parameters.pictureFormat
        Loger.d("PictureFormat: $pictureFormat")

        val supportPreviewSizes = parameters.supportedPreviewSizes
        supportPreviewSizes.forEach {
            Loger.d("SupportPreviewSizes: ${it.width} ,${it.height}")
        }
        val supportPictureSize = parameters.supportedPictureSizes
        supportPictureSize.forEach {
            Loger.d("SupportPictureSizes: ${it.width} ,${it.height}")
        }
        val supportPictureFormats = parameters.supportedPictureFormats
        supportPictureFormats.forEach {
            Loger.d("SupportPictureFormats: = $it")
        }
    }

}