
package com.riddkasep.ai

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var web: WebView? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun params(w: Int, h: Int) =
        WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(24)
            y = dp(120)
            softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

    private fun showBubble() {
        if (bubble != null) return

        val b = TextView(this).apply {
            text = "RK"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF087E9B.toInt())
        }

        val p = params(dp(58), dp(58))
        var startX = 0
        var startY = 0
        var moved = false

        b.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX.toInt()
                    startY = event.rawY.toInt()
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - startX
                    val dy = event.rawY.toInt() - startY

                    if (kotlin.math.abs(dx) > 5 ||
                        kotlin.math.abs(dy) > 5) moved = true

                    p.x = (p.x - dx).coerceAtLeast(0)
                    p.y = (p.y + dy).coerceAtLeast(0)

                    wm.updateViewLayout(b, p)
                    startX = event.rawX.toInt()
                    startY = event.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) showPanel(p.x, p.y)
                    true
                }
                else -> true
            }
        }

        bubble = b
        wm.addView(b, p)
    }

    private fun showPanel(x: Int, y: Int) {
        bubble?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bubble = null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF101318.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFF087E9B.toInt())
        }

        val title = TextView(this).apply {
            text = "  riddKasep AI  •  Geser di sini"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }

        val close = TextView(this).apply {
            text = "  ×  "
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(42)))

        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            isFocusable = true
            isFocusableInTouchMode = true
            webViewClient = WebViewClient()
            setBackgroundColor(0xFF101318.toInt())
            loadUrl("file:///android_asset/index.html")
        }

        val resize = TextView(this).apply {
            text = "↘  Tarik untuk ubah ukuran"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF202530.toInt())
        }

        root.addView(header)
        root.addView(wv, LinearLayout.LayoutParams(
            -1, 0, 1f
        ))
        root.addView(resize, LinearLayout.LayoutParams(-1, dp(30)))

        val p = params(dp(330), dp(480)).apply {
            x = x.coerceAtLeast(0)
            y = y.coerceAtLeast(0)
        }

        panel = root
        web = wv
        panelParams = p

        close.setOnClickListener {
            closePanel()
            showBubble()
        }

        var lastX = 0
        var lastY = 0

        header.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = e.rawX.toInt()
                    lastY = e.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = (p.x - (e.rawX.toInt() - lastX)).coerceAtLeast(0)
                    p.y = (p.y + (e.rawY.toInt() - lastY)).coerceAtLeast(0)
                    lastX = e.rawX.toInt()
                    lastY = e.rawY.toInt()
                    wm.updateViewLayout(root, p)
                    true
                }
                else -> true
            }
        }

        var resizeX = 0
        var resizeY = 0

        resize.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    resizeX = e.rawX.toInt()
                    resizeY = e.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.width = (p.width + e.rawX.toInt() - resizeX)
                        .coerceIn(dp(240), dp(600))
                    p.height = (p.height + e.rawY.toInt() - resizeY)
                        .coerceIn(dp(200), dp(800))
                    resizeX = e.rawX.toInt()
                    resizeY = e.rawY.toInt()
                    wm.updateViewLayout(root, p)
                    true
                }
                else -> true
            }
        }

        wm.addView(root, p)
        wv.requestFocus()
    }

    private fun closePanel() {
        panel?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        web?.destroy()
        panel = null
        web = null
        panelParams = null
    }

    override fun onDestroy() {
        closePanel()
        bubble?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
