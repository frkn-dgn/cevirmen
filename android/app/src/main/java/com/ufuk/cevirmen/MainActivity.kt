package com.ufuk.cevirmen

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var pageReady = false
    private var pendingText: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // index.html uygulamanin icinden https://appassets.androidplatform.net adresiyle sunulur
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this)
        setContentView(web)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.userAgentString = web.settings.userAgentString + " CevirmenApp/1"
        web.addJavascriptInterface(NativeBridge(), "CevirmenNative")

        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Uygulama disi linkler (aistudio, groq vb.) tarayicida acilir
                if (request.url.host == "appassets.androidplatform.net") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (_: Exception) {
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                pendingText?.let { deliverText(it) }
                pendingText = null
            }
        }

        // Geri tusu: metin varsa once metni siler, yoksa uygulamayi kapatir
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(window.cevirmenBack && window.cevirmenBack()) ? 'y' : 'n'") { r ->
                    if (r == null || !r.contains("y")) finish()
                }
            }
        })

        pendingText = extractText(intent)
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val text = extractText(intent) ?: return
        if (pageReady) deliverText(text) else pendingText = text
    }

    private fun extractText(intent: Intent?): String? {
        if (intent == null) return null
        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
        return text?.takeIf { it.isNotBlank() }
    }

    private fun deliverText(text: String) {
        web.evaluateJavascript(
            "window.cevirmenSetText && window.cevirmenSetText(" + JSONObject.quote(text) + ")",
            null
        )
    }

    // Sayfadaki JavaScript'in cagirdigi yerel fonksiyonlar
    inner class NativeBridge {
        private val clipboard: ClipboardManager
            get() = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        @JavascriptInterface
        fun paste(): String {
            return try {
                clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)?.coerceToText(this@MainActivity)?.toString() ?: ""
            } catch (_: Exception) {
                ""
            }
        }

        @JavascriptInterface
        fun copy(text: String) {
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText("Çeviri", text))
            } catch (_: Exception) {
            }
        }

        // Sayfa temasi degisince durum cubugu ve alt cubuk renklerini esitle
        @JavascriptInterface
        fun setDark(dark: Boolean) {
            runOnUiThread {
                val color = if (dark) Color.parseColor("#0F131B") else Color.parseColor("#F4F6FA")
                window.statusBarColor = color
                window.navigationBarColor = color
                web.setBackgroundColor(color)
                val ctl = WindowCompat.getInsetsController(window, window.decorView)
                ctl.isAppearanceLightStatusBars = !dark
                ctl.isAppearanceLightNavigationBars = !dark
            }
        }
    }
}
