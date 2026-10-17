package com.ytlibre.app.ui.main

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ytlibre.app.R
import com.ytlibre.app.databinding.ActivityMainBinding
import com.ytlibre.app.ui.watch.PlayerWebView
import com.ytlibre.app.ui.watch.WatchFragment

/**
 * Activité unique : barre d'outils, navigation par onglets et lecture en
 * Picture-in-Picture lorsque l'appareil le permet.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.searchFragment, R.id.libraryFragment, R.id.settingsFragment)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val host = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = host.navController
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id == R.id.watchFragment) View.GONE else View.VISIBLE
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "ytlibre" && data.host == "video") {
            val videoId = data.lastPathSegment?.takeIf { it.isNotBlank() } ?: return
            openVideo(videoId, null, null, null)
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    /** Ouvre une vidéo (utilisé par le deep link). */
    fun openVideo(videoId: String, title: String?, channelTitle: String?, channelId: String?) {
        navController.navigate(
            R.id.watchFragment,
            bundleOf(
                WatchFragment.ARG_VIDEO_ID to videoId,
                WatchFragment.ARG_TITLE to title,
                WatchFragment.ARG_CHANNEL_TITLE to channelTitle,
                WatchFragment.ARG_CHANNEL_ID to channelId
            )
        )
    }

    /** Bascule en Picture-in-Picture (API 26+, support OEM variable). */
    fun enterPlayerPictureInPicture(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }.getOrDefault(false)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PlayerWebView.isVideoPlaying) {
            enterPlayerPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        binding.toolbar.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        binding.bottomNav.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
    }

    companion object {
        fun watchUri(videoId: String): Uri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
    }
}
