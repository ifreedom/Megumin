package com.sqrtf.megumin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.sqrtf.common.activity.BaseActivity
import com.sqrtf.common.api.ApiHelper


class PlayerActivity : BaseActivity() {
    val playerView: SimpleExoPlayerView by lazy { findViewById(R.id.player_content) as SimpleExoPlayerView }

    val mHidePart2Runnable = Runnable {
        playerView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    val mShowPart2Runnable = Runnable {
        supportActionBar?.show()
    }


    val mHideHandler = Handler()
    var bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
    var videoTrackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory(bandwidthMeter)
    var trackSelector: TrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
    val player: SimpleExoPlayer by lazy { ExoPlayerFactory.newSimpleInstance(this, trackSelector) }

    var lastPlayWhenReady = false
    var controllerVisibility = View.VISIBLE

    companion object {
        fun intent(context: Context, url: String, id: String, id2: String): Intent {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(INTENT_KEY_URL, url)
            intent.putExtra(RESULT_KEY_ID, id)
            intent.putExtra(RESULT_KEY_ID_2, id2)
            return intent
        }

        private val INTENT_KEY_URL = "INTENT_KEY_URL"
        private val UI_ANIMATION_DELAY = 100

        val RESULT_KEY_ID = "PlayerActivity:RESULT_KEY_ID"
        val RESULT_KEY_ID_2 = "PlayerActivity:RESULT_KEY_ID_2"
        val RESULT_KEY_POSITION = "PlayerActivity:RESULT_KEY_POSITION"
        val RESULT_KEY_DURATION = "PlayerActivity:RESULT_KEY_DURATION"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        checkMultiWindowMode()

        val url = intent.getStringExtra(INTENT_KEY_URL)
        if (TextUtils.isEmpty(url)) throw IllegalArgumentException("Required url")

        val fixedUrl = ApiHelper.fixHttpUrl(url)
        Log.i(this.localClassName, "playing:" + fixedUrl)

        playerView.setControllerVisibilityListener {
            controllerVisibility = it
            if (it == View.VISIBLE) {
                show()
            } else {
                hide()
            }
        }
        playerView.player = player

        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, BuildConfig.APPLICATION_ID))
        val extractorsFactory = DefaultExtractorsFactory()
        val videoSource = ExtractorMediaSource(Uri.parse(fixedUrl), dataSourceFactory, extractorsFactory, null, null)
        player.prepare(videoSource)

        player.playWhenReady = true
        lastPlayWhenReady = true
    }

    private fun checkMultiWindowMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playerView.fitsSystemWindows = isInMultiWindowMode
        }
    }

    override fun onBackPressed() {
//        if (controllerVisibility == View.VISIBLE) {
//            playerView.hideController()
//        } else {
//            super.onBackPressed()
//        }
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_SPACE)
                && controllerVisibility != View.VISIBLE) {
            playerView.showController()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        player.playWhenReady = lastPlayWhenReady
    }

    override fun onStop() {
        super.onStop()
        lastPlayWhenReady = player.playWhenReady
        player.playWhenReady = false
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        playerView.fitsSystemWindows = isInMultiWindowMode
    }

    override fun onDestroy() {
        super.onDestroy()
        player.playWhenReady = false
        player.release()
    }

    override fun finish() {
        val resultCode = if (player.currentPosition > 0) Activity.RESULT_OK else Activity.RESULT_CANCELED
        val i = intent
        i.putExtra(RESULT_KEY_POSITION, player.currentPosition)
        i.putExtra(RESULT_KEY_DURATION, player.duration)

        setResult(resultCode, i)
        super.finish()
    }

    private fun hide() {
        supportActionBar?.hide()

        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        playerView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }
}
