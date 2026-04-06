package com.example.bingo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tencent.smtt.sdk.QbSdk

class MainActivity : AppCompatActivity() {

    private val fixedUrl = "https://lotto.auzo.tw/tv/bingoTv/index.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        QbSdk.initX5Environment(applicationContext, object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {}
            override fun onViewInitFinished(isX5: Boolean) {}
        })

        val intent = android.content.Intent(this, WebViewActivity::class.java).apply {
            putExtra("EXTRA_URL", fixedUrl)
        }
        startActivity(intent)
        finish()
    }
}