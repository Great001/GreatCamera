package com.example.hancher.greatcamera

import android.annotation.TargetApi
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.File

/**
 * 编码器
 * Created by liaohaicong on 2019/4/24.
 */
class Encoder(var format: MediaFormat) {

    companion object {
        private val DEQUEUE_TIMEOUT = 10 * 1000L
    }

    init {
        initEncoder()
    }

    private var codec: MediaCodec? = null
    private var configSuccess = false

    //编码开关
    private var stopEncode = false

    private var isEncoding = false

    //合成
    private var muxer: MediaMuxer? = null
    private var trackIndex: Int = 0

    //编码数据源,null标识编码完成
    private var sourceBytes: ByteArray? = kotlin.ByteArray(0)

    private var curPresentationsUs: Long = 0

    private var endOfStream = false

    /**
     * 编码合成线程
     */
    private var encodeRunnable: Runnable = Runnable {
        val bufferInfo = MediaCodec.BufferInfo()
        val nv12 = ByteArray((1920 * 1080 * 1.5).toInt())
        while (true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!endOfStream) {
                    //申请一个ByteBuffer装数据
                    val inputBufferIndex = codec?.dequeueInputBuffer(DEQUEUE_TIMEOUT)
                    val inputBuffer = codec?.getInputBuffer(inputBufferIndex!!) ?: continue
                    if (sourceBytes == null) {
                        endOfStream = true
                        codec?.queueInputBuffer(inputBufferIndex!!, 0, 0, curPresentationsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        Loger.d("EndOfStream")
                    } else if (sourceBytes!!.isNotEmpty()) {
                        ColorFormatTransformer.NV21ToNV12(sourceBytes, nv12, 1920, 1080)
                        inputBuffer.clear()
                        inputBuffer.put(nv12)
                        codec?.queueInputBuffer(inputBufferIndex!!, 0, nv12.size, curPresentationsUs, 0)
                    }
                }
                var outputBufferIndex = codec?.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT)
                when (outputBufferIndex) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        //超时了，等待一下
                        Loger.d("info try again later")
                        Thread.sleep(200)
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Loger.d("info output format changed")
                        format = codec?.outputFormat!!
                        initMuxer()
                    }
                    else -> {
                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            Loger.d("数据都已经编码完了，哈哈！！！")
                            codec?.stop()
                            codec?.release()
                            muxer?.stop()
                            muxer?.release()
                            return@Runnable
                        }
                        val outputBuffer = codec?.getOutputBuffer(outputBufferIndex!!)
                        //合成
                        Loger.d("合成一帧数据")
                        muxer?.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                        codec?.releaseOutputBuffer(outputBufferIndex!!, false)
                    }
                }
            }
        }
    }

    private fun initEncoder() {
        initEncoderCodec()
        configEncoder()
    }

    private fun initEncoderCodec() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val codecName = getEncodeCodecName()
            codec = MediaCodec.createByCodecName(codecName)
        } else {
            val mineType = format.getString(MediaFormat.KEY_MIME)
            codec = MediaCodec.createEncoderByType(mineType)
        }
        Loger.d("编码器名称： ${codec?.name}")
    }

    private fun configEncoder() {
        try {
            codec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            configSuccess = true
            Loger.d("编码器配置成功")
        } catch (e: Exception) {
            e.printStackTrace()
            configSuccess = false
            Loger.d("Exception: configEncoderFail")
        }
    }

    fun start() {
        if (!configSuccess) {
            Loger.d("编码器未初始化好")
            return
        }
        Loger.d("编码器开始工作")
        stopEncode = false
        endOfStream = false
        codec?.start()
        //开启线程执行
        Thread(encodeRunnable).start()
    }


    fun stop() {
        Loger.d("编码器停止")
        signalEndOfStream()
        stopEncode = true
    }

    fun drainData(byteArray: ByteArray?, presentationTimeUs: Long) {
        if (!stopEncode) {
            sourceBytes = byteArray
            curPresentationsUs = presentationTimeUs
        }
    }

    private fun signalEndOfStream() {
        Loger.d("signalEndOfStream")
        drainData(null, curPresentationsUs)
    }

    private fun initMuxer() {
        val saveFile = File(FolderHelper.getCurVideoSavePath())
        val parentFile = saveFile.parentFile
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (saveFile.exists()) {
            saveFile.delete()
        }
        saveFile.createNewFile()
        muxer = MediaMuxer(saveFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        trackIndex = muxer?.addTrack(format)!!
        muxer?.start()
        Loger.d("启动Muxer合成")
    }


    @TargetApi(21)
    private fun getEncodeCodecName(): String {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        return codecList.findEncoderForFormat(format)
    }
}