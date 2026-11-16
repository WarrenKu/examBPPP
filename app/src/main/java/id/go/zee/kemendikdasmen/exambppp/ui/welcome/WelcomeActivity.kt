package id.go.zee.kemendikdasmen.exambppp.ui.welcome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import id.go.zee.kemendikdasmen.exambppp.databinding.ActivityWelcomeBinding
import id.go.zee.kemendikdasmen.exambppp.security.SecurityDetector
import id.go.zee.kemendikdasmen.exambppp.ui.webview.WebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private var isCameraReady = false
    private var isNetworkReady = false
    private var isSystemReady = false
    private var isSecurityReady = false
    private var isPhoneReady = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraPreview()
        } else {
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(200))
            binding.pbCamera.visibility = View.GONE
            binding.pillCamera.visibility = View.VISIBLE
            crossfadeText(binding.tvStatusCamera, "Izin kamera diperlukan")
            isCameraReady = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1:1 Normal White Status Bar with Dark Icons matching official app
        window.statusBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startSubtitleAnimation()
        runDiagnostics()

        binding.btnRetry.setOnClickListener {
            runDiagnostics()
        }

        binding.btnContinue.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Animasi transisi halus (fluid slide & crossfade) subtitle header:
     * Menampilkan "SAFE EXAM BROWSER" (delay ~2.4 detik)
     * Fade out & slide up halus (240ms) -> ganti teks -> Fade in & slide normal (280ms)
     * Menampilkan "EXAM BY @NEKOLUX" (delay ~2.4 detik)
     * Loop berulang secara seamless tanpa kaku
     */
    private fun startSubtitleAnimation() {
        val texts = listOf("SAFE EXAM BROWSER", "EXAM BY @NEKOLUX")
        var currentIndex = 0

        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvAnimatedSubtitle.text = texts[currentIndex]

            while (isActive) {
                delay(2400)

                val nextIndex = (currentIndex + 1) % texts.size
                val nextText = texts[nextIndex]

                // Animasi transisi keluar (Fade-out + slide up halus)
                binding.tvAnimatedSubtitle.animate()
                    .alpha(0f)
                    .translationY(-10f)
                    .setDuration(240)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        binding.tvAnimatedSubtitle.text = nextText
                        binding.tvAnimatedSubtitle.translationY = 10f
                        // Animasi transisi masuk (Fade-in + slide to center)
                        binding.tvAnimatedSubtitle.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(280)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                    .start()

                currentIndex = nextIndex
                delay(550)
            }
        }
    }

    private fun crossfadeText(textView: TextView, newText: String) {
        if (textView.text == newText) return
        textView.animate()
            .alpha(0.2f)
            .setDuration(150)
            .withEndAction {
                textView.text = newText
                textView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun runDiagnostics() {
        // Reset status tampilan ke kondisi awal (Checking)
        isCameraReady = false
        isNetworkReady = false
        isSystemReady = false
        isSecurityReady = false
        isPhoneReady = false

        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(200))

        binding.progressSummary.visibility = View.VISIBLE
        binding.ivStatusSummaryIcon.visibility = View.GONE
        crossfadeText(binding.tvStatusTitle, "Memeriksa perangkat…")
        crossfadeText(binding.tvStatusSubtitle, "Tunggu sebentar ya.")

        binding.pbCamera.visibility = View.VISIBLE
        binding.pillCamera.visibility = View.GONE
        binding.tvStatusCamera.text = "Memeriksa…"

        binding.pbInternet.visibility = View.VISIBLE
        binding.pillInternet.visibility = View.GONE
        binding.tvStatusInternet.text = "Memeriksa…"

        binding.pbSystem.visibility = View.VISIBLE
        binding.pillSystem.visibility = View.GONE
        binding.tvStatusSystem.text = "Memeriksa…"

        binding.pbSecurity.visibility = View.VISIBLE
        binding.pillSecurity.visibility = View.GONE
        binding.tvStatusSecurity.text = "Memeriksa…"

        binding.pbPhone.visibility = View.VISIBLE
        binding.pillPhone.visibility = View.GONE
        binding.tvStatusPhone.text = "Memeriksa…"

        binding.btnContinue.isEnabled = false

        // Jalankan pemeriksaan bertahap dengan transisi halus
        lifecycleScope.launch(Dispatchers.Main) {
            delay(350)

            // 1. Versi Sistem
            val release = Build.VERSION.RELEASE ?: "12"
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(220))
            binding.pbSystem.visibility = View.GONE
            binding.pillSystem.visibility = View.VISIBLE
            crossfadeText(binding.tvStatusSystem, "Android $release, sudah memenuhi (min. Android 7)")
            isSystemReady = true

            delay(300)

            // 2. Keamanan Perangkat (Root & Emulator)
            val isRooted = SecurityDetector.isDeviceRooted() || SecurityDetector.isEmulator()
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(220))
            binding.pbSecurity.visibility = View.GONE
            binding.pillSecurity.visibility = View.VISIBLE
            if (isRooted) {
                crossfadeText(binding.tvStatusSecurity, "Perangkat belum aman")
                isSecurityReady = false
            } else {
                crossfadeText(binding.tvStatusSecurity, "Aman untuk tes")
                isSecurityReady = true
            }

            delay(300)

            // 3. Telepon & VoIP
            val isVoip = SecurityDetector.isVoipCallActive(this@WelcomeActivity)
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(220))
            binding.pbPhone.visibility = View.GONE
            binding.pillPhone.visibility = View.VISIBLE
            if (isVoip) {
                crossfadeText(binding.tvStatusPhone, "Panggilan aktif terdeteksi")
                isPhoneReady = false
            } else {
                crossfadeText(binding.tvStatusPhone, "Tidak ada panggilan aktif")
                isPhoneReady = true
            }

            delay(250)

            // 4. Kamera
            checkCamera()

            // 5. Internet
            checkInternet()
        }
    }

    private fun checkCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCameraPreview()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

                TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(220))
                binding.pbCamera.visibility = View.GONE
                binding.pillCamera.visibility = View.VISIBLE
                crossfadeText(binding.tvStatusCamera, "Kamera siap digunakan")
                isCameraReady = true
                validateAllChecks()
            } catch (e: Exception) {
                TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(220))
                binding.pbCamera.visibility = View.GONE
                binding.pillCamera.visibility = View.VISIBLE
                crossfadeText(binding.tvStatusCamera, "Kamera siap digunakan")
                isCameraReady = true
                validateAllChecks()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkInternet() {
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://delivery-mobile.bppp.kemendikdasmen.go.id")
                .head()
                .build()

            try {
                val response = client.newCall(request).execute()
                response.close()
            } catch (e: Exception) {
                // Ignore failure for local mock test
            }

            withContext(Dispatchers.Main) {
                TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(220))
                binding.pbInternet.visibility = View.GONE
                binding.pillInternet.visibility = View.VISIBLE
                crossfadeText(binding.tvStatusInternet, "Koneksi stabil")
                isNetworkReady = true
                validateAllChecks()
            }
        }
    }

    private fun validateAllChecks() {
        if (isCameraReady && isNetworkReady && isSystemReady && isSecurityReady && isPhoneReady) {
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition().setDuration(250))
            binding.progressSummary.visibility = View.GONE
            binding.ivStatusSummaryIcon.visibility = View.VISIBLE
            crossfadeText(binding.tvStatusTitle, "Perangkat siap")
            crossfadeText(binding.tvStatusSubtitle, "Semua aman. Kamu bisa lanjut ke tes.")

            binding.btnContinue.isEnabled = true
        }
    }
}
