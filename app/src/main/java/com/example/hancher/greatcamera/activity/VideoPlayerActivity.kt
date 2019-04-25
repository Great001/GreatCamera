package com.example.hancher.greatcamera.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.FragmentActivity
import android.view.SurfaceHolder
import android.view.View
import com.example.hancher.greatcamera.R
import com.example.hancher.greatcamera.codec.Decoder
import com.example.hancher.greatcamera.utils.Loger
import kotlinx.android.synthetic.main.activity_video_player.*

/**
 * 视频播放页
 * Created by liaohaicong on 2019/4/25.
 */
class VideoPlayerActivity : FragmentActivity(), SurfaceHolder.Callback, Decoder.OnPlayCompleteListener {

    var decoder: Decoder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        videoPlayer.holder.addCallback(this)

        videoPlayer.setOnClickListener {
            if (decoder!!.isPlaying()) {
                decoder?.pause()
                pauseView.visibility = View.VISIBLE
            } else {
                decoder?.resume()
                pauseView.visibility = View.GONE
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Loger.d("VideoPlayer:SurfaceCreated")
        startPlay(holder)
    }


    private fun startPlay(holder: SurfaceHolder?) {
        decoder = Decoder()
        decoder?.configDecoder(holder!!.surface)
        decoder?.setOnPlayCompleteListener(this)
        decoder?.startPlay()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Loger.d("VideoPlayer:SurfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Loger.d("VideoPlayer:SurfaceDestroyed")
        decoder?.stopPlay()
    }

    override fun onPlayComplete() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable {
            pauseView.visibility = View.VISIBLE
        })
    }
}