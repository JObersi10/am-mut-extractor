package com.ammut.extractor

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * A tiny single-purpose phone app: log into Apple Music in a WebView, read back the
 * media-user-token (MUT) the web player stores, and show it with a Copy button so you can
 * paste it into Apple Music TV's :8080 token page. Runs on a phone, so the keyboard just works
 * (unlike the Fire TV WebView).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var status: TextView
    private lateinit var tokenView: TextView
    private lateinit var copyBtn: Button
    private lateinit var restartBtn: Button
    private var captured: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        status = TextView(this).apply {
            text = "Sign in to Apple Music below"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#111114"))
            setPadding(32, 28, 32, 28)
            textSize = 15f
        }
        root.addView(status, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // Result panel (hidden until a token is found).
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#0B2A12"))
            setPadding(32, 24, 32, 24)
        }
        tokenView = TextView(this).apply {
            setTextColor(Color.parseColor("#7CFFA0"))
            textSize = 12f
            setTextIsSelectable(true)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        panel.addView(tokenView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        copyBtn = Button(this).apply { text = "Copy token" ; setOnClickListener { copyToken() } }
        restartBtn = Button(this).apply { text = "Sign in again" ; setOnClickListener { restart() } }
        btnRow.addView(copyBtn, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        btnRow.addView(restartBtn, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        panel.addView(btnRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = 16 })
        root.addView(panel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        this.panel = panel

        web = WebView(this)
        val webHolder = FrameLayout(this)
        webHolder.addView(web, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(webHolder, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        setContentView(root)
        configureWeb()
        web.loadUrl(LOGIN_URL)
        pollForToken()
    }

    private lateinit var panel: LinearLayout

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWeb() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (captured == null) status.text = "Sign in with your Apple ID"
            }
        }
        // MusicKit sign-in may open a popup (idmsa.apple.com) — give it its own WebView so it works.
        web.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView?, dialog: Boolean, gesture: Boolean, resultMsg: Message?): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                val popup = WebView(this@MainActivity)
                popup.settings.javaScriptEnabled = true
                popup.settings.domStorageEnabled = true
                popup.settings.setSupportMultipleWindows(true)
                popup.settings.javaScriptCanOpenWindowsAutomatically = true
                CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true)
                popup.webViewClient = WebViewClient()
                popup.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(w: WebView?) { runCatching { (web.parent as FrameLayout).removeView(popup); popup.destroy() } }
                }
                (web.parent as FrameLayout).addView(popup, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                transport.webView = popup
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    private fun pollForToken() {
        val handler = android.os.Handler(mainLooper)
        val tick = object : Runnable {
            override fun run() {
                if (captured != null) return
                web.evaluateJavascript(TOKEN_JS) { raw ->
                    val t = raw?.trim('"')?.replace("\\\"", "\"").orEmpty()
                    if (captured == null && t.isNotBlank() && t != "null" && t.length > 20) {
                        captured = t
                        CookieManager.getInstance().flush()
                        showToken(t)
                    }
                }
                handler.postDelayed(this, 1500)
            }
        }
        handler.postDelayed(tick, 1500)
    }

    private fun showToken(t: String) {
        status.text = "Got it! Copy this token into Apple Music TV (:8080)"
        status.setBackgroundColor(Color.parseColor("#0B2A12"))
        tokenView.text = t
        panel.visibility = View.VISIBLE
        web.visibility = View.GONE
    }

    private fun copyToken() {
        val t = captured ?: return
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("media-user-token", t))
        Toast.makeText(this, "Token copied", Toast.LENGTH_SHORT).show()
    }

    private fun restart() {
        captured = null
        panel.visibility = View.GONE
        web.visibility = View.VISIBLE
        status.setBackgroundColor(Color.parseColor("#111114"))
        status.text = "Sign in with your Apple ID"
        CookieManager.getInstance().removeAllCookies(null)
        web.clearCache(true)
        web.loadUrl(LOGIN_URL)
        pollForToken()
    }

    override fun onDestroy() {
        runCatching { web.destroy() }
        super.onDestroy()
    }

    companion object {
        private const val LOGIN_URL = "https://music.apple.com/us/login"

        /** Read the MUT from the cookie, localStorage, or MusicKit's live instance. "" until present. */
        private const val TOKEN_JS = """
        (function(){
          try { var m = document.cookie.match(/media-user-token=([^;]+)/); if (m && m[1]) return m[1]; } catch(e){}
          try {
            for (var i=0;i<localStorage.length;i++){
              var k = localStorage.key(i);
              if (k && k.toLowerCase().indexOf('media-user-token') >= 0) {
                var v = localStorage.getItem(k); if (v) return v;
              }
            }
          } catch(e){}
          try {
            if (window.MusicKit && MusicKit.getInstance && MusicKit.getInstance().musicUserToken)
              return MusicKit.getInstance().musicUserToken;
          } catch(e){}
          return "";
        })();
        """
    }
}
