package com.riddkasep.ai
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.*
class MainActivity : Activity() {
 private lateinit var web: WebView
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  web = WebView(this)
  web.settings.javaScriptEnabled = true
  web.settings.domStorageEnabled = true
  web.webChromeClient = WebChromeClient()
  web.webViewClient = WebViewClient()
  web.addJavascriptInterface(AndroidBridge(), "RiddKasepAndroid")
  setContentView(web)
  web.loadUrl("file:///android_asset/index.html")
 }
 inner class AndroidBridge {
  @JavascriptInterface fun requestPermission(name: String): Boolean {
   if (name == "overlay" && !Settings.canDrawOverlays(this@MainActivity)) openAndroidSettings()
   return name != "overlay" || Settings.canDrawOverlays(this@MainActivity)
  }
  @JavascriptInterface fun openAndroidSettings() {
   startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
  }
  @JavascriptInterface fun getPermissionSnapshotJson(): String =
   "{\"overlay\":" + Settings.canDrawOverlays(this@MainActivity) + "}"
  @JavascriptInterface fun startFloatingService() {
   runOnUiThread {
    if (Settings.canDrawOverlays(this@MainActivity)) startService(Intent(this@MainActivity, FloatingService::class.java))
    else openAndroidSettings()
   }
  }
  @JavascriptInterface fun stopFloatingService() { stopService(Intent(this@MainActivity, FloatingService::class.java)) }
 }
 override fun onDestroy() { web.destroy(); super.onDestroy() }
}
