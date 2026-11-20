package com.riddkasep.ai
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
class FloatingService : Service() {
 private lateinit var wm: WindowManager
 private var bubble: TextView? = null
 private var panel: WebView? = null
 override fun onCreate() { super.onCreate(); wm = getSystemService(WINDOW_SERVICE) as WindowManager; showBubble() }
 private fun params(w: Int, h: Int) = WindowManager.LayoutParams(w,h,
  WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
  PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = 24; y = 120 }
 private fun showBubble() {
  if (bubble != null) return
  val b = TextView(this).apply {
   text = "RK"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
   setBackgroundColor(0xFF087E9B.toInt()); setOnClickListener { showPanel() }
  }
  bubble = b; wm.addView(b, params(58,58))
 }
 private fun showPanel() {
  if (panel != null) { try { wm.removeView(panel) } catch (_: Exception) {}; panel = null; showBubble(); return }
  bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }; bubble = null
  val v = WebView(this).apply {
   settings.javaScriptEnabled = true; settings.domStorageEnabled = true
   webViewClient = WebViewClient(); setBackgroundColor(0xFF101318.toInt())
   loadUrl("file:///android_asset/index.html")
  }
  panel = v; wm.addView(v, params(330,480))
  v.setOnLongClickListener { try { wm.removeView(v) } catch (_: Exception) {}; panel = null; showBubble(); true }
 }
 override fun onDestroy() {
  panel?.let { try { wm.removeView(it); it.destroy() } catch (_: Exception) {} }
  bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
  panel = null; bubble = null; super.onDestroy()
 }
 override fun onBind(intent: Intent?): IBinder? = null
}
