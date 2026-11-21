
package com.riddkasep.ai

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Color

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var web: WebView? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var opacity = 100

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun params(width: Int, height: Int) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(20)
            y = dp(100)
            softInputMode =
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

    private fun showBubble() {
        if (bubble != null) return

        val b = TextView(this).apply {
            text = "RK"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(0, 126, 155))
            elevation = dp(8).toFloat()
        }

        val p = params(dp(58), dp(58))
        bubble = b

        b.setOnTouchListener(object : android.view.View.OnTouchListener {
            var startX = 0
            var startY = 0
            var downX = 0f
            var downY = 0f
            var moved = false

            override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x
                        startY = p.y
                        downX = event.rawX
                        downY = event.rawY
                        moved = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) > dp(5) ||
                            kotlin.math.abs(dy) > dp(5)) moved = true

                        p.x = (startX + dx).coerceAtLeast(0)
                        p.y = (startY + dy).coerceAtLeast(0)
                        try {
                            wm.updateViewLayout(b, p)
                        } catch (_: Exception) {}
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!moved) showPanel(p.x, p.y)
                        return true
                    }
                }
                return true
            }
        })

        wm.addView(b, p)
    }

    private fun showPanel(x: Int, y: Int) {
        if (panel != null) return

        bubble?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bubble = null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24, 27, 34))
            elevation = dp(12).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(5), dp(6), dp(5))
            setBackgroundColor(Color.rgb(0, 105, 130))
        }

        val title = TextView(this).apply {
            text = "riddKasep AI"
            textSize = 15f
            setTextColor(Color.WHITE)
        }

        val close = Button(this).apply {
            text = "X"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
        }

        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(close, LinearLayout.LayoutParams(dp(48), dp(42)))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setBackgroundColor(Color.rgb(35, 40, 49))
        }

        val opacityLabel = TextView(this).apply {
            text = "Opacity 100%"
            textSize = 11f
            setTextColor(Color.WHITE)
        }

        val slider = SeekBar(this).apply {
            max = 80
            progress = 80
        }

        controls.addView(
            opacityLabel,
            LinearLayout.LayoutParams(dp(90), dp(40))
        )
        controls.addView(
            slider,
            LinearLayout.LayoutParams(0, dp(40), 1f)
        )

        val wv = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            isFocusable = true
            isFocusableInTouchMode = true
            webViewClient = WebViewClient()
            setBackgroundColor(Color.rgb(16, 19, 24))
            loadUrl("file:///android_asset/index.html")
        }

        val resize = TextView(this).apply {
            text = "↘ Tarik untuk ubah ukuran"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(0, 105, 130))
        }

        root.addView(header)
        root.addView(controls)
        root.addView(
            wv,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        root.addView(resize, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(32)
        ))

        val p = params(dp(330), dp(500))
        p.x = x.coerceAtLeast(0)
        p.y = y.coerceAtLeast(0)

        panel = root
        web = wv
        panelParams = p

        close.setOnClickListener { closePanel() }

        slider.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                opacity = progress + 20
                opacityLabel.text = "Opacity $opacity%"
                root.alpha = opacity / 100f
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        header.setOnTouchListener(dragListener(p, root))

        resize.setOnTouchListener(object : android.view.View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var startW = 0
            var startH = 0

            override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startW = p.width
                        startH = p.height
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        p.width = (startW + (event.rawX - downX).toInt())
                            .coerceIn(dp(240), dp(600))
                        p.height = (startH + (event.rawY - downY).toInt())
                            .coerceIn(dp(250), dp(800))
                        try { wm.updateViewLayout(root, p) }
                        catch (_: Exception) {}
                        return true
                    }
                }
                return true
            }
        })

        wm.addView(root, p)
    }

    private fun dragListener(
        p: WindowManager.LayoutParams,
        view: android.view.View
    ) = object : android.view.View.OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0

        override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = p.x
                    startY = p.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = (startX + (event.rawX - downX).toInt()).coerceAtLeast(0)
                    p.y = (startY + (event.rawY - downY).toInt()).coerceAtLeast(0)
                    try { wm.updateViewLayout(view, p) }
                    catch (_: Exception) {}
                    return true
                }
            }
            return true
        }
    }

    private fun closePanel() {
        val root = panel
        if (root != null) {
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
                web?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
                wm.removeView(root)
            } catch (_: Exception) {}
        }

        web?.destroy()
        web = null
        panel = null
        panelParams = null
        showBubble()
    }

    override fun onDestroy() {
        try { panel?.let { wm.removeView(it) } } catch (_: Exception) {}
        try { bubble?.let { wm.removeView(it) } } catch (_: Exception) {}

        web?.destroy()
        panel = null
        bubble = null
        web = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
