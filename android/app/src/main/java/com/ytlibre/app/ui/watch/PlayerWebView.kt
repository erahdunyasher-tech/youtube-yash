package com.ytlibre.app.ui.watch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.google.gson.JsonParser

/**
 * Lecteur vidéo intégré.
 *
 * Utilise le lecteur officiel YouTube embarqué (page « /embed » servie par
 * YouTube dans une WebView). Aucune extraction de flux, aucun contournement de
 * publicité, aucun téléchargement : l'application affiche simplement le lecteur
 * mis à disposition par YouTube, avec ses règles (publicité, géo-restrictions,
 * disponibilité des vidéos).
 */
class PlayerWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onPlayerReady()
        fun onPlayingChanged(isPlaying: Boolean)
        fun onPlayerError(message: String)
    }

    var listener: Listener? = null

    private val webView: WebView = WebView(context)
    private var currentVideoId: String? = null
    private var isPlaying = false

    init {
        setBackgroundColor(Color.BLACK)
        addView(
            webView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.setBackgroundColor(Color.BLACK)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectBridge(view)
            }
        }
        webView.addJavascriptInterface(PlayerBridge(), BRIDGE_NAME)
        acceptYouTubeCookies()
    }

    /** Consentement YouTube : nécessaire pour l'embed dans l'EU. */
    private fun acceptYouTubeCookies() {
        runCatching {
            val manager = CookieManager.getInstance()
            manager.setAcceptCookie(true)
            manager.setAcceptThirdPartyCookies(webView, true)
            manager.setCookie(
                "https://www.youtube.com",
                "CONSENT=YES+cb; Domain=.youtube.com; Path=/; Secure; SameSite=None"
            )
        }
    }

    fun loadVideo(videoId: String, autoplay: Boolean = true) {
        if (videoId.isBlank()) return
        currentVideoId = videoId
        val url = buildEmbedUrl(videoId, autoplay)
        webView.loadUrl(url)
    }

    fun play() {
        webView.evaluateJavascript("window.__ytLibreCommand && window.__ytLibreCommand('playVideo');", null)
    }

    fun pause() {
        webView.evaluateJavascript("window.__ytLibreCommand && window.__ytLibreCommand('pauseVideo');", null)
    }

    fun release() {
        runCatching {
            webView.removeJavascriptInterface(BRIDGE_NAME)
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
        currentVideoId = null
    }

    private fun buildEmbedUrl(videoId: String, autoplay: Boolean): String = buildString {
        append("https://www.youtube.com/embed/")
        append(videoId)
        append("?enablejsapi=1&playsinline=1&rel=0&fs=0&modestbranding=1&iv_load_policy=3")
        append("&origin=https://www.youtube.com")
        append("&autoplay=")
        append(if (autoplay) "1" else "0")
    }

    /**
     * Injecte le pont JavaScript : relais des événements du lecteur officiel
     * (onStateChange) et envoi de commandes (play/pause) au lecteur.
     */
    private fun injectBridge(view: WebView?) {
        view?.evaluateJavascript(BRIDGE_SCRIPT, null)
    }

    private inner class PlayerBridge {

        @android.webkit.JavascriptInterface
        fun onReady() {
            post {
                listener?.onPlayerReady()
                listener?.onPlayingChanged(false)
            }
        }

        @android.webkit.JavascriptInterface
        fun onEvent(payload: String) {
            val state = parsePlayerState(payload) ?: return
            post {
                val playing = state == PLAYER_STATE_PLAYING
                if (playing != isPlaying) {
                    isPlaying = playing
                    PlayerWebView.updatePlayingState(playing)
                    listener?.onPlayingChanged(playing)
                }
            }
        }
    }

    private fun parsePlayerState(payload: String): Int? = runCatching {
        val json = JsonParser.parseString(payload).asJsonObject
        val event = json.get("event")?.asString ?: return null
        if (!event.startsWith("onStateChange")) return null
        json.getAsJsonObject("info")?.get("playerState")?.asInt
    }.getOrNull()

    companion object {
        private const val BRIDGE_NAME = "YTLibre"
        private const val PLAYER_STATE_PLAYING = 1

        /** État de lecture partagé avec l'activité (Picture-in-Picture). */
        @Volatile
        var isVideoPlaying: Boolean = false
            private set

        fun updatePlayingState(playing: Boolean) {
            isVideoPlaying = playing
        }

        private val BRIDGE_SCRIPT = """
(function () {
  if (window.__ytLibreInstalled) { return; }
  window.__ytLibreInstalled = true;

  function findPlayerFrame() {
    var frames = document.getElementsByTagName('iframe');
    for (var i = 0; i < frames.length; i++) {
      var src = frames[i].src || '';
      if (src.indexOf('youtube') !== -1) { return frames[i]; }
    }
    return frames.length > 0 ? frames[0] : null;
  }

  window.__ytLibreCommand = function (func, args) {
    var frame = findPlayerFrame();
    if (!frame || !frame.contentWindow) { return false; }
    frame.contentWindow.postMessage(
      JSON.stringify({ event: 'command', func: func, args: args || [] }), '*'
    );
    return true;
  };

  window.addEventListener('message', function (event) {
    try {
      var data = event.data;
      if (!data || typeof data !== 'object' || !data.event) { return; }
      if (data.event.indexOf('on') === 0 && window.YTLibre && window.YTLibre.onEvent) {
        window.YTLibre.onEvent(JSON.stringify(data));
      }
    } catch (error) { /* silencieux */ }
  });

  var attempts = 0;
  var timer = setInterval(function () {
    attempts += 1;
    if (window.YTLibre && window.YTLibre.onReady) {
      window.YTLibre.onReady();
      clearInterval(timer);
    }
    if (attempts > 40) { clearInterval(timer); }
  }, 250);
})();
""".trimIndent()
    }
}
