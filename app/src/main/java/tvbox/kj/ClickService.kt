package tvbox.kj

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ClickService : AccessibilityService() {

    private lateinit var handler: Handler
    private var tapReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        Log.d(TAG, "Service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected - registering receiver")
        // 註冊 BroadcastReceiver 接收來自 APP 的點擊指令
        tapReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val x = intent?.getFloatExtra(EXTRA_X, 0f) ?: return
                val y = intent?.getFloatExtra(EXTRA_Y, 0f) ?: return
                val delay = intent?.getLongExtra(EXTRA_DELAY, 0) ?: 0L
                Log.d(TAG, "Broadcast received: tap($x, $y) delay=$delay")
                handler.postDelayed({ performTap(x, y) }, delay)
            }
        }
        registerReceiver(tapReceiver, IntentFilter(ACTION_TAP))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        tapReceiver?.let { unregisterReceiver(it) }
        Log.d(TAG, "Service destroyed")
    }

    private fun performTap(x: Float, y: Float) {
        try {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Gesture completed at ($x, $y)")
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Gesture CANCELLED at ($x, $y)")
                }
            }, null)
            Log.d(TAG, "dispatchGesture called")
        } catch (e: Exception) {
            Log.e(TAG, "performTap error: ${e.message}")
        }
    }

    companion object {
        const val ACTION_TAP = "tvbox.kj.ACTION_TAP"
        const val EXTRA_X = "x"
        const val EXTRA_Y = "y"
        const val EXTRA_DELAY = "delay"
        private const val TAG = "ClickService"
    }
}
