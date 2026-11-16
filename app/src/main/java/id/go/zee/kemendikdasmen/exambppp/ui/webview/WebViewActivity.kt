package id.go.zee.kemendikdasmen.exambppp.ui.webview

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.go.zee.kemendikdasmen.exambppp.R
import id.go.zee.kemendikdasmen.exambppp.bridge.AndroidBridge
import id.go.zee.kemendikdasmen.exambppp.databinding.ActivityWebviewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Hardened Exam Lockdown Container
 * Reverse-Engineered from id.go.kemendikdasmen.exambppp.ui.webview.WebViewActivity
 */
class WebViewActivity : AppCompatActivity(), AndroidBridge.BridgeListener {

    private lateinit var binding: ActivityWebviewBinding
    private var isExamActive = false
    private var statusMonitorJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val pct = if (scale > 0 && level >= 0) (level * 100) / scale else 0
                binding.examStatusBar.tvBatteryIcon.text = if (isCharging) "⚡" else "🔋"
                binding.examStatusBar.tvBatteryValue.text = "$pct%"
            }
        }
    }

    companion object {
        const val TARGET_URL = "https://delivery-mobile.bppp.kemendikdasmen.go.id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enforce Window Security: Block Screenshots & Screen Recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // 2. Prevent Floating Overlays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }

        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Enter Immersive Fullscreen Mode
        hideSystemUI()

        // 4. Configure Hardened WebView
        setupHardenedWebView()

        // 5. Setup Error Action Listener
        binding.nativeErrorContainer.btnErrorAction.setOnClickListener {
            binding.nativeErrorContainer.root.visibility = View.GONE
            binding.webView.reload()
        }

        // 6. Load Initial Delivery Endpoint
        binding.webView.loadUrl(TARGET_URL)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        startStatusMonitor()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        statusMonitorJob?.cancel()
    }

    private fun startStatusMonitor() {
        statusMonitorJob?.cancel()
        statusMonitorJob = lifecycleScope.launch {
            while (isActive) {
                updateNetworkStatus()
                updateRamStatus()
                delay(2500)
            }
        }
    }

    private fun updateNetworkStatus() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)

            if (caps == null) {
                binding.examStatusBar.tvNetworkIcon.text = "⚠️"
                binding.examStatusBar.tvNetworkValue.text = "Terputus"
            } else {
                binding.examStatusBar.tvNetworkIcon.text = "📶"
                binding.examStatusBar.tvNetworkValue.text = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Seluler"
                    else -> "Stabil"
                }
            }
        } catch (e: Exception) {
            binding.examStatusBar.tvNetworkIcon.text = "📶"
            binding.examStatusBar.tvNetworkValue.text = "Stabil"
        }
    }

    private fun updateRamStatus() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)

            val totalGb = memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
            val usedGb = (memInfo.totalMem - memInfo.availMem).toDouble() / (1024 * 1024 * 1024)

            binding.examStatusBar.tvRamIcon.text = "💾"
            binding.examStatusBar.tvRamValue.text = String.format(Locale.US, "%.1f/%.1f GB", usedGb, totalGb)
        } catch (e: Exception) {
            binding.examStatusBar.tvRamIcon.text = "💾"
            binding.examStatusBar.tvRamValue.text = "—"
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupHardenedWebView() {
        val settings = binding.webView.settings

        // Enable JavaScript for CBT Engine
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        // Lockdown File Access
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false

        // Disallow Multi-Window
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false

        // 6. Inject Official Secret User-Agent Suffix
        val defaultUserAgent = settings.userAgentString
        settings.userAgentString = defaultUserAgent + AndroidBridge.USER_AGENT_TOKEN

        // 7. Inject AndroidBridge Javascript Interface
        val bridge = AndroidBridge(this)
        binding.webView.addJavascriptInterface(bridge, "AndroidBridge")

        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: ""
                // Restrict navigation only to authorized BPPP domains
                return if (url.contains("bppp.kemendikdasmen.go.id")) {
                    false
                } else {
                    Toast.makeText(this@WebViewActivity, "Navigasi keluar sistem dilarang!", Toast.LENGTH_SHORT).show()
                    true
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    binding.nativeErrorContainer.root.visibility = View.VISIBLE
                    binding.nativeErrorContainer.errorMessage.text = error?.description ?: getString(R.string.error_no_internet)
                    binding.nativeErrorContainer.btnErrorAction.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        } else if (isExamActive) {
            // Anti-Cheat: Leaving the app during active test triggers violation
            Toast.makeText(this, getString(R.string.exit_warning_msg), Toast.LENGTH_LONG).show()
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        super.onMultiWindowModeChanged(isInMultiWindowMode)
        if (isInMultiWindowMode) {
            AlertDialog.Builder(this)
                .setTitle(R.string.split_screen_blocked_title)
                .setMessage(R.string.split_screen_blocked_msg)
                .setCancelable(false)
                .setPositiveButton("Kembali ke Layar Penuh") { _, _ -> hideSystemUI() }
                .show()
        }
    }

    override fun onBackPressed() {
        if (isExamActive) {
            onRequestNativeExit()
        } else if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // AndroidBridge.BridgeListener Callbacks
    override fun onExamStarted() {
        isExamActive = true
        runOnUiThread {
            Toast.makeText(this, "Sesi Ujian BPPP Dimulai (Lockdown Aktif)", Toast.LENGTH_SHORT).show()
            binding.webView.evaluateJavascript(
                "if(window.ExamCallback && typeof window.ExamCallback.notifyExamStarted === 'function'){ window.ExamCallback.notifyExamStarted(); }",
                null
            )
        }
    }

    override fun onExamFinished() {
        isExamActive = false
        runOnUiThread {
            Toast.makeText(this, "Sesi Ujian Selesai. Data tersimpan.", Toast.LENGTH_SHORT).show()
            binding.webView.evaluateJavascript(
                "if(window.ExamCallback && typeof window.ExamCallback.notifyExamFinished === 'function'){ window.ExamCallback.notifyExamFinished(); }",
                null
            )
        }
    }

    override fun onRouteChanged(route: String) {
        runOnUiThread {
            // ExamBPPP tracks paths: /login, /peserta/start_test, /peserta/finish_test, /peserta/logout
            if (route == "/peserta/start_test") {
                isExamActive = true
            } else if (route == "/peserta/finish_test" || route == "/peserta/logout") {
                isExamActive = false
            }
        }
    }

    override fun onRequestNativeExit() {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(R.string.exit_confirmation_title)
                .setMessage(R.string.exit_confirmation_msg)
                .setPositiveButton("Ya, Keluar") { _, _ ->
                    // Clear all session traces
                    binding.webView.clearCache(true)
                    binding.webView.clearFormData()
                    binding.webView.clearHistory()
                    binding.webView.loadUrl("about:blank")
                    finish()
                }
                .setNegativeButton("Lanjutkan Ujian", null)
                .show()
        }
    }
}
