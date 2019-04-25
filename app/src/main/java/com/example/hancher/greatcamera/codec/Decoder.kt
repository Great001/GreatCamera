package com.example.hancher.greatcamera.codec

import android.annotation.TargetApi
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import com.example.hancher.greatcamera.utils.FolderHelper
import com.example.hancher.greatcamera.utils.Loger
import java.io.File

/**
 * 解码器
 * Created by liaohaicong on 2019/4/25.
 */
class Decoder {

    companion object {
        val TIME_OUT = 100 * 1000L
    }

    //解码
    private var decoder: MediaCodec? = null
    private var format: MediaFormat? = null

    private var initSuccess: Boolean = false

    //解析
    private var extractor: MediaExtractor? = null
    private var trackIndex: Int = 0

    private var endOfStream = false

    private var isPlaying: Boolean = false
    private var pausePlay: Boolean = false
    private var stopPlay: Boolean = false

    private var decodeThread: Thread? = null

    private var playCompleteListener: OnPlayCompleteListener? = null

    init {
        initMediaExtractor()
        initDecoder()
    }

    private fun initDecoder() {
        getDecoderCodec()
    }


    private fun getDecoderCodec() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findDecoderForFormat()
        } else {
            val mine = format!!.getString(MediaFormat.KEY_MIME)
            decoder = MediaCodec.createDecoderByType(mine)
        }
    }

    @TargetApi(21)
    private fun findDecoderForFormat() {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codecName = codecList.findDecoderForFormat(format)
        decoder = MediaCodec.createByCodecName(codecName)
    }

    fun configDecoder(surface: Surface) {
        try {
            decoder?.configure(format, surface, null, 0)
            initSuccess = true
            Loger.d("配置解码器成功")
        } catch (e: Exception) {
            Loger.d("Decoder config fail!!!")
        }
    }

    fun startPlay() {
        if (!initSuccess) {
            Loger.d("解码器未初始化成功！！！")
            return
        }
        Loger.d("开始播放")
        stopPlay = false
        decoder?.start()
        decodeThread = Thread(decodeRunnable)
        decodeThread!!.start()
        isPlaying = true
    }

    fun resume() {
        if (endOfStream) {
            Loger.d("已经播完了")
            return
        }
        pausePlay = false
        decodeThread?.interrupt()
        Loger.d("继续播放")
    }

    fun pause() {
        pausePlay = true
        Loger.d("暂停播放")
    }


    fun stopPlay() {
        Loger.d("停止播放")
        stopPlay = true
        decoder?.stop()
        decoder?.release()
        extractor?.release()
        isPlaying = false
    }

    private fun initMediaExtractor() {
        Loger.d("初始化MediaExtractor")
        extractor = MediaExtractor()
        val videoPath = FolderHelper.getVideoSaveDir() + File.separator + "test.MP4"
        extractor?.setDataSource(videoPath)
        val trackCount = extractor?.trackCount
        for (index in 0 until trackCount!!) {
            format = extractor?.getTrackFormat(index)!!
            if (format?.getString(MediaFormat.KEY_MIME)!!.startsWith("video")) {
                trackIndex = index
                extractor!!.selectTrack(index)
                break
            }
        }
    }

    fun isPlaying() = isPlaying


    private val decodeRunnable = Runnable {
        val bufferInfo = MediaCodec.BufferInfo()
        while (!stopPlay) {
            if (pausePlay) {
                try {
                    isPlaying = false
                    Thread.sleep(Int.MAX_VALUE.toLong())
                } catch (e: InterruptedException) {
                    isPlaying = true
                    Loger.d("InterruptedException")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!endOfStream) {
                    val inputBufferIndex = decoder?.dequeueInputBuffer(TIME_OUT)!!
//                    val inputBufferIndex2 = decoder?.dequeueInputBuffer(50000)!!
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder?.getInputBuffer(inputBufferIndex)!!
                        val size = extractor?.readSampleData(inputBuffer, 0)!!
                        //慢速编码考虑加帧
//                        if (inputBufferIndex2 >= 0) {
//                            val inputBuffer2 = decoder?.getInputBuffer(inputBufferIndex2)!!
//                            extractor?.readSampleData(inputBuffer2, 0)!!
//                        }
//                        extractor?.readSampleData(inputBuffer, 0)!!

                        if (size >= 0) {
                            val presentationTimeUs = extractor!!.sampleTime
                            val flags = extractor!!.sampleFlags
                            decoder?.queueInputBuffer(inputBufferIndex, 0, size, presentationTimeUs, flags)
//                            if (inputBufferIndex2 >= 0) {
//                                Loger.d("添加第2帧重复帧")
//                                decoder?.queueInputBuffer(inputBufferIndex2, 0, size, presentationTimeUs, flags)
//                            }
                            extractor?.advance()
                            //快速播放，跳帧
//                            for (i in 0 until 2) {
//                                extractor?.advance()
//                            }
                            Loger.d("读取填充一帧数据")
                        } else {
                            Loger.d("数据读取完毕")
                            endOfStream = true
                            decoder?.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        }
                    }
                }
                val outputBufferIndex = decoder?.dequeueOutputBuffer(bufferInfo, TIME_OUT)!!
                when (outputBufferIndex) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        Loger.d("解码器：info try again later")
                        Thread.sleep(200)
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        Loger.d("解码器：output format changed")
                    }
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Loger.d("解码器：OutputBufferChanged")
                    }
                    else -> {
                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            Loger.d("视频解码完成，播放顺利结束")
                            playCompleteListener?.onPlayComplete()
                            return@Runnable
                        }
                        Loger.d("解码器：解码出一帧数据:$outputBufferIndex")
                        decoder?.releaseOutputBuffer(outputBufferIndex, true)
                        //慢速播放
//                        Thread.sleep(10)
                    }
                }

            }
        }
    }

    fun setOnPlayCompleteListener(listener: OnPlayCompleteListener) {
        this.playCompleteListener = listener
    }

    interface OnPlayCompleteListener {
        fun onPlayComplete()
    }

}