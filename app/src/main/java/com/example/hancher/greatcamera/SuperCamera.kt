package com.example.hancher.greatcamera

import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.view.SurfaceView
import java.io.File

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

    //拍照
    private var camera: Camera? = null
    private var isFront: Boolean = false
    private var cameraOrientation: Int = 0


    //录像
    //方案1：MediaRecorder
    private var recorder: MediaRecorder? = null
    private var isRecording: Boolean = false

    //方案2：MediaCodec编码，数据源PreviewCallback
    private var encoder: Encoder? = null
    private var encodeStartTime: Long = 0

    private val previewCallback = Camera.PreviewCallback { data, camera ->
        //Loger.d("onPreviewFrame回调")
        camera.addCallbackBuffer(data)

        val presentationTimeUS = (System.currentTimeMillis() - encodeStartTime) * 1000
        encoder?.drainData(data, presentationTimeUS)
    }

    fun openCamera(isFront: Boolean, surfaceView: SurfaceView) {
        openCamera(isFront, false, surfaceView)
    }

    fun openCamera(isFront: Boolean, withCallback: Boolean, surfaceView: SurfaceView) {
        this.isFront = isFront
        val facing = if (isFront) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK
        //可能会阻塞，最好放在异步线程中
        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(facing, cameraInfo)
        cameraOrientation = cameraInfo.orientation
        camera = Camera.open(facing)
        val parameters = camera!!.parameters
        printCameraParameters(parameters)
        //设置预览尺寸，优先1920*1080
        val previewSize = choosePreviewSize(parameters)
        parameters.setPreviewSize(previewSize.width, previewSize.height)
        //设置照片尺寸，优先1920*1080
        val pictureSize = choosePictureSize(parameters)
        parameters.setPictureSize(pictureSize.width, pictureSize.height)
        //设置预览帧率，控制在25帧内
        val previewFps = choosePreviewFpsRange(parameters)
        parameters.setPreviewFpsRange(previewFps[0], previewFps[1])
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
        if (withCallback) {
            //设置预览回调
            val bufferSize = previewSize.width * previewSize.height * (ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8f)
            for (i in 0..1) {
                val byteArray = ByteArray(bufferSize.toInt())
                camera?.addCallbackBuffer(byteArray)
            }
            camera?.setPreviewCallbackWithBuffer(previewCallback)
        }
        camera?.startPreview()
    }

    fun toggleCamera(preview: SurfaceView) {
        if (camera != null) {
            closeCamera()
            openCamera(!isFront, preview)
        }
    }

    fun takePicture(pictureCallback: Camera.PictureCallback) {
        if (camera != null) {
            camera?.takePicture(Camera.ShutterCallback { }, null, pictureCallback)
        }
    }

    fun closeCamera() {
        try {
            camera?.stopPreview()
            camera?.release()
            camera = null
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun recordVideo() {
        val outputFile = File(FolderHelper.getCurVideoSavePath())
        val parentFile = outputFile.parentFile
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.createNewFile()
        if (camera == null) {
            Loger.d("相机有问题，不能录像")
            return
        }
        try {
            recorder = MediaRecorder()
            //必须在相机停止预览前才能获取到parameters，否则获取到的为空
            val videoSize = chooseEncodeVideoSize(camera!!.parameters)
            //必须停止预览
            camera?.stopPreview()
            camera?.unlock()
            recorder?.setCamera(camera)

            //下面三行代码顺序不能改
            recorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder?.setOutputFormat(MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder?.setAudioChannels(2)
            recorder?.setAudioSamplingRate(44100)
            //视频宽高一定要是相机支持的
            if (videoSize != null) {
                recorder?.setVideoSize(videoSize.width, videoSize.height)
            } else {
                recorder?.setVideoSize(1920, 1080)
            }
            //帧率25fps可以了
            recorder?.setVideoFrameRate(25)
            //位率大概设5M/s，低于这个值清晰度很难保证
            recorder?.setVideoEncodingBitRate(5000000)
            recorder?.setOutputFile(outputFile.absolutePath)
            recorder?.prepare()
            recorder?.start()
            isRecording = true
            Loger.d("开始录像")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            try {
                //必须及时释放相机资源，不然一直占用，导致相机都不能用啦,
                //经过测试,camera unlock后，如果有crash，必须要重新lock之后再释放才有效！！！
                recorder?.release()
                camera?.lock()
                closeCamera()
                recorder = null
                isRecording = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecord() {
        if (!isRecording) {
            return
        }
        try {
            recorder?.stop()
            Loger.d("停止录像")
        } catch (e: Exception) {
            e.printStackTrace()
            closeCamera()
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
        }
    }

    fun recordVideoWithEncoder() {
        try {
            //mime:"video/avc" 兼容性比较好
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080)
            //选择颜色格式，注意兼容性
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25)
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 8000000)

            encoder = Encoder(videoFormat)
            encoder?.start()
            encodeStartTime = System.currentTimeMillis()
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            closeCamera()
        }
    }

    fun stopRecordVideoWithEncoder() {
        try {
            encoder?.stop()
            isRecording = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isRecording() = isRecording

    fun isCameraAvailable(): Boolean {
        return camera != null
    }

    private fun choosePreviewSize(parameters: Camera.Parameters): Camera.Size {
        val supportPreviewSize = parameters.supportedPreviewSizes
        supportPreviewSize.forEach {
            if (it.width == 1920 && it.height == 1080) {
                return it
            }
        }
        return parameters.previewSize
    }

    private fun choosePictureSize(parameters: Camera.Parameters): Camera.Size {
        val supportPictureSize = parameters.supportedPictureSizes
        supportPictureSize.forEach {
            if (it.width == 1920 && it.height == 1080) {
                return it
            }
        }
        return parameters.pictureSize
    }

    private fun chooseEncodeVideoSize(parameters: Camera.Parameters): Camera.Size? {
        val supportVideoSize = parameters.supportedVideoSizes
        if (supportVideoSize != null && supportVideoSize.isNotEmpty()) {
            supportVideoSize.forEach {
                if (it.width == 1920 && it.height == 1080) {
                    return it
                }
            }
        } else {
            return null
        }
        return supportVideoSize[0]
    }

    private fun choosePreviewFpsRange(parameters: Camera.Parameters): IntArray {
        val supportFpsRange = parameters.supportedPreviewFpsRange
        supportFpsRange.forEach {
            if (it[0] > 0 && it[1] <= 30)
                return it
        }
        val range = IntArray(2)
        parameters.getPreviewFpsRange(range)
        return range
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
        val supportVideoSize = parameters.supportedVideoSizes
        if (supportVideoSize != null && supportVideoSize.isNotEmpty()) {
            supportVideoSize.forEach {
                Loger.d("SupportVideoSize: ${it.width}, ${it.height}")
            }
        }
        val supportPictureFormats = parameters.supportedPictureFormats
        supportPictureFormats.forEach {
            Loger.d("SupportPictureFormats: = $it")
        }
        val supportPreviewFormats = parameters.supportedPreviewFormats
        supportPreviewFormats.forEach {
            Loger.d("SupportPreviewFormats: = $it")
        }
    }

}