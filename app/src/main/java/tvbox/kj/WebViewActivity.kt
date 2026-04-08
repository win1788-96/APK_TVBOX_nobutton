package tvbox.kj

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient

class WebViewActivity : AppCompatActivity() {

    private var currentUrl: String? = null
    private lateinit var webView: WebView
    private lateinit var btnRotate: ImageButton

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        currentUrl = intent.getStringExtra("EXTRA_URL")
        webView = findViewById(R.id.webView)
        btnRotate = findViewById(R.id.btnRotate)

        setupWebView()

        btnRotate.setOnClickListener {
            RotationManager.toggleRotation(this)
        }

        findViewById<View>(R.id.rootView).setOnTouchListener { _, _ ->
            btnRotate.visibility = View.VISIBLE
            false
        }
        
        webView.setOnTouchListener { _, _ ->
            btnRotate.visibility = View.VISIBLE
            false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setAppCacheEnabled(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                view?.loadUrl("javascript:(function() { " +
                        "document.body.style.margin='0'; " +
                        "document.body.style.padding='0'; " +
                        "document.documentElement.style.overflow='hidden'; " +
                        "})()")
            }
        }
        
        currentUrl?.let { webView.loadUrl(it) }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
