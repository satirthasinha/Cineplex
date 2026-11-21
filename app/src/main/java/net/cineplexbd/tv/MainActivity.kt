package net.cineplexbd.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var root: FrameLayout
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    companion object {
        private const val HOME_URL = "http://103.136.200.66/"
        private const val SERVER_HOST = "103.136.200.66"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(9, 13, 24)
        window.navigationBarColor = Color.rgb(9, 13, 24)

        root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(9, 13, 24)) }
        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(9, 13, 24))
            isFocusable = true
            isFocusableInTouchMode = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.blockNetworkImage = false
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    val scheme = uri.scheme ?: return true
                    if (scheme == "mailto" || scheme == "tel") {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        return true
                    }
                    // Keep CinePlex pages and media on the server inside the TV app.
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    injectTvFocusStyles(view)
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame) {
                        Toast.makeText(this@MainActivity, "Cannot reach CinePlex. Check your ISP/network connection.", Toast.LENGTH_LONG).show()
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (fullscreenView != null) {
                        callback.onCustomViewHidden()
                        return
                    }
                    fullscreenView = view
                    fullscreenCallback = callback
                    root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    webView.visibility = View.GONE
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
                }

                override fun onHideCustomView() { hideFullscreen() }
            }

            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                Toast.makeText(this@MainActivity, "Download link opened by CinePlex: $mimeType", Toast.LENGTH_SHORT).show()
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }.onFailure {
                    Toast.makeText(this@MainActivity, "No app available to open this download.", Toast.LENGTH_SHORT).show()
                }
            })
        }
        root.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        webView.requestFocus(View.FOCUS_DOWN)
        if (savedInstanceState == null) webView.loadUrl(HOME_URL) else webView.restoreState(savedInstanceState)
    }

    private fun injectTvFocusStyles(view: WebView) {
        val js = """(function(){
          if(document.getElementById('cineplex-tv-focus-style')) return;
          var s=document.createElement('style'); s.id='cineplex-tv-focus-style';
          s.textContent='a,button,input,select,textarea,[role=button]{outline-offset:4px} a:focus,button:focus,input:focus,select:focus,textarea:focus,[role=button]:focus{outline:3px solid #38bdf8!important;box-shadow:0 0 0 5px rgba(56,189,248,.22)!important} html{scroll-behavior:smooth}';
          document.head.appendChild(s);
          document.querySelectorAll('a,button,input,select,textarea,[role=button]').forEach(function(e){if(!e.hasAttribute('tabindex') && e.tagName!=='A' && e.tagName!=='BUTTON' && e.tagName!=='INPUT' && e.tagName!=='SELECT' && e.tagName!=='TEXTAREA') e.tabIndex=0;});
        })();"""
        view.evaluateJavascript(js, null)
    }

    private fun hideFullscreen() {
        fullscreenView?.let { root.removeView(it) }
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        webView.visibility = View.VISIBLE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        webView.requestFocus()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (fullscreenView != null && event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            hideFullscreen(); return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (fullscreenView != null) { hideFullscreen(); return }
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
