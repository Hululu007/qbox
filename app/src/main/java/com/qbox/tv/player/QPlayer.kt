package com.qbox.tv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.qbox.tv.model.PlayerResult
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** ExoPlayer 封装：单实例、header 支持、直链/HLS 自适应。 */
class QPlayer(context: Context) {

    private val httpFactory = OkHttpDataSource.Factory(
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    ).setUserAgent("Mozilla/5.0 (Linux; Android 11; TV) AppleWebKit/537.3")

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()

    fun play(url: String, headers: Map<String, String>?) {
        httpFactory.setDefaultRequestProperties(headers ?: emptyMap())
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun resume() { if (!player.isPlaying) player.play() }
    fun pause() { player.pause() }

    fun release() {
        player.release()
    }
}
