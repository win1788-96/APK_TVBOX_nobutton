package tvbox.kj

import android.annotation.SuppressLint
import android.hardware.input.InputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
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
                // 移除頁面邊距
                view?.loadUrl("javascript:(function() { " +
                        "document.body.style.margin='0'; " +
                        "document.body.style.padding='0'; " +
                        "document.documentElement.style.overflow='hidden'; " +
                        "})()")

                // 延遲執行：關閉彈窗 + 自動點擊六宮格按鈕
                Handler(Looper.getMainLooper()).postDelayed({
                    // 1. 關閉 SweetAlert2 免費版彈窗
                    view?.loadUrl("javascript:(function() { " +
                        "if(typeof Swal!=='undefined'){Swal.close();} " +
                        "var sw=document.querySelector('.swal2-container'); if(sw)sw.remove(); " +
                        "})()")

                    // 2. 自動切換到六宮格
                    //    循環模式：2格 → 4格 → 6格 → 2格...
                    //    頁面預設是 2格，點擊 2 次切換到 6格
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Starting grid switch sequence...")
                        // 第 1 次點擊：2格 → 4格
                        injectTap(1080f, 18f)
                        Log.d(TAG, "Click 1: 2-grid → 4-grid")
                        
                        Handler(Looper.getMainLooper()).postDelayed({
                            // 第 2 次點擊：4格 → 6格
                            injectTap(1080f, 18f)
                            Log.d(TAG, "Click 2: 4-grid → 6-grid")
                        }, 3000)
                    }, 15000)
                    
                    // 3. 每小時刷新頁面重新檢測
                    //    防止頁面長時間運行後狀態異常
                    scheduleHourlyRefresh()
                }, 3000)
            }
        }

        currentUrl?.let { webView.loadUrl(it) }
    }

    /**
     * 使用 InputManager.injectInputEvent 注入觸摸事件
     * 需要 INJECT_EVENTS 權限（系統 App 專屬）
     * 這是內核級注入，X5 WebView 的 Laya Canvas 可以識別
     */
    private fun injectTap(x: Float, y: Float) {
        try {
            // 先列出 InputManager 的所有方法（調試用）
            val imClass = Class.forName("android.hardware.input.InputManager")
            val getInstance = imClass.getMethod("getInstance")
            val im = getInstance.invoke(null)
            
            Log.d(TAG, "InputManager class: ${im.javaClass.name}")
            Log.d(TAG, "InputManager methods:")
            for (m in im.javaClass.methods) {
                if (m.name.contains("inject", ignoreCase = true)) {
                    Log.d(TAG, "  ${m.name}(${m.parameterTypes.joinToString { it.simpleName }})")
                }
            }
            
            // 嘗試找到 injectInputEvent 方法
            val injectMethod = im.javaClass.methods.find { it.name == "injectInputEvent" }
            if (injectMethod != null) {
                val paramTypes = injectMethod.parameterTypes
                Log.d(TAG, "Found injectInputEvent with params: ${paramTypes.joinToString { it.name }}")
                
                val now = SystemClock.uptimeMillis()
                
                // ACTION_DOWN
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
                down.source = InputDevice.SOURCE_TOUCHSCREEN
                injectMethod.invoke(im, down, 2) // 2 = INJECT_MODE_WAIT
                down.recycle()
                Log.d(TAG, "injectTap DOWN at ($x, $y) OK")
                
                // ACTION_UP
                val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0)
                up.source = InputDevice.SOURCE_TOUCHSCREEN
                injectMethod.invoke(im, up, 2)
                up.recycle()
                Log.d(TAG, "injectTap UP at ($x, $y) OK")
            } else {
                Log.e(TAG, "injectInputEvent method not found!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "injectTap failed: ${e.message}", e)
        }
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

    /**
     * 截取右上角按鈕區域（座標 980,0 到 1080,100）
     * 檢查是否已是六宮格模式
     * - 六宮格模式：顯示「2格返回按鈕」→ 區域較小（< 5KB）→ 不點
     * - 非六宮格（2格/4格）：顯示「6格切換按鈕」→ 區域較大（>= 5KB）→ 點擊切換
     */
    private fun checkToolbarRegion(): Boolean {
        try {
            // 截取右上角區域 (100x100 像素)
            val bitmap = android.graphics.Bitmap.createBitmap(
                100, 100,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            canvas.translate(-980f, 0f) // 偏移截取右上角
            webView.draw(canvas)
            
            // 計算區域文件大小
            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val sizeKB = byteArrayOutputStream.size() / 1024
            
            Log.d(TAG, "Toolbar region size: ${sizeKB}KB")
            
            bitmap.recycle()
            
            // 如果區域大於 5KB，說明「6宫格切換按鈕」存在 → 不是六宮格 → 需要點擊
            // 如果區域小於 5KB，說明已是六宮格 → 不需要點擊
            return sizeKB >= 5
        } catch (e: Exception) {
            Log.e(TAG, "checkToolbarRegion failed: ${e.message}")
            return true // 失敗時假設需要點擊
        }
    }

    /**
     * 檢查當前狀態，如果不是六宮格則點擊切換
     * 循環模式：2格 → 4格 → 6格 → 2格...
     * 持續檢測直到確認是六宮格為止
     */
    private fun checkAndSwitchIfNeeded() {
        Log.d(TAG, "Checking current grid mode...")
        checkAndSwitchLoop(1)
    }

    /**
     * 循環檢測並切換，最多 3 次
     */
    private fun checkAndSwitchLoop(attempt: Int) {
        if (attempt > 3) {
            Log.d(TAG, "Finished switching attempts")
            return
        }
        
        val needsClick = checkToolbarRegion()
        
        if (!needsClick) {
            Log.d(TAG, "Already in 6-grid mode, no need to click")
            return
        }
        
        Log.d(TAG, "Not in 6-grid mode (attempt $attempt), clicking...")
        injectTap(1080f, 18f)
        
        // 等待 2 秒後再次檢查（不阻塞主线程）
        Handler(Looper.getMainLooper()).postDelayed({
            checkAndSwitchLoop(attempt + 1)
        }, 2000)
    }

    /**
     * 每小時刷新頁面並重新檢測狀態
     * 防止長時間運行後頁面狀態異常
     */
    private fun scheduleHourlyRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Hourly refresh: reloading page...")
            currentUrl?.let { webView.loadUrl(it) }
        }, 3600000) // 1 小時 = 3600000 毫秒
    }

    companion object {
        private const val TAG = "WebViewActivity"
    }
}
