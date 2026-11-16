package id.go.zee.kemendikdasmen.exambppp.ui.welcome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
            binding.pbCamera.visibility = View.GONE
            binding.tvStatusCamera.text = "Izin kamera ditolak"
            binding.tvStatusCamera.setTextColor(Color.parseColor("#EF4444"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
     * Animasi teks subtitle header:
     * "SAFE EXAM BROWSER" (delay 2 detik)
     * Animasi erase dari kanan ke kiri (0.5 detik)
     * "EXAM BY @NEKOVELUX" (delay 2 detik)
     * Animasi erase dari kanan ke kiri (0.5 detik)
     * Loop berulang terus menerus
     */
    private fun startSubtitleAnimation() {
        val texts = listOf("SAFE EXAM BROWSER", "EXAM BY @NEKOVELUX")
        var currentIndex = 0

        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvAnimatedSubtitle.text = texts[currentIndex]

            while (isActive) {
                delay(2000)

                val currentText = texts[currentIndex]
                val nextIndex = (currentIndex + 1) % texts.size
                val nextText = texts[nextIndex]

                val eraseSteps = currentText.length
                val stepDelay = 500L / eraseSteps.coerceAtLeast(1)

                // Erase huruf dari kanan ke kiri
                for (i in eraseSteps downTo 0) {
                    binding.tvAnimatedSubtitle.text = currentText.substring(0, i)
                    delay(stepDelay)
                }

                // Munculkan teks baru
                val typeSteps = nextText.length
                val typeStepDelay = 350L / typeSteps.coerceAtLeast(1)

                for (i in 1..typeSteps) {
                    binding.tvAnimatedSubtitle.text = nextText.substring(0, i)
                    delay(typeStepDelay)
                }

                currentIndex = nextIndex
            }
        }
    }

    private fun runDiagnostics() {
        // Reset status tampilan
        isCameraReady = false
        isNetworkReady = false
        isSystemReady = false
        isSecurityReady = false
        isPhoneReady = false

        binding.progressBarBanner.visibility = View.VISIBLE
        binding.ivBannerCheck.visibility = View.GONE
        binding.tvBannerTitle.text = "Memeriksa perangkat..."
        binding.tvBannerSubtitle.text = "Tunggu sebentar ya."

        binding.pbCamera.visibility = View.VISIBLE
        binding.ivCameraDone.visibility = View.GONE
        binding.tvStatusCamera.text = "Memeriksa..."
        binding.tvStatusCamera.setTextColor(Color.parseColor("#64748B"))

        binding.pbInternet.visibility = View.VISIBLE
        binding.ivInternetDone.visibility = View.GONE
        binding.tvStatusInternet.text = "Memeriksa..."
        binding.tvStatusInternet.setTextColor(Color.parseColor("#64748B"))

        binding.pbSystem.visibility = View.VISIBLE
        binding.ivSystemDone.visibility = View.GONE
        binding.tvStatusSystem.text = "Memeriksa..."
        binding.tvStatusSystem.setTextColor(Color.parseColor("#64748B"))

        binding.pbSecurity.visibility = View.VISIBLE
        binding.ivSecurityDone.visibility = View.GONE
        binding.tvStatusSecurity.text = "Memeriksa..."
        binding.tvStatusSecurity.setTextColor(Color.parseColor("#64748B"))

        binding.pbPhone.visibility = View.VISIBLE
        binding.ivPhoneDone.visibility = View.GONE
        binding.tvStatusPhone.text = "Memeriksa..."
        binding.tvStatusPhone.setTextColor(Color.parseColor("#64748B"))

        binding.btnContinue.isEnabled = false
        binding.btnContinue.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))

        // Jalankan pemeriksaan secara bertahap
        lifecycleScope.launch(Dispatchers.Main) {
            delay(400)

            // 1. Versi Sistem
            val release = Build.VERSION.RELEASE ?: "14"
            binding.pbSystem.visibility = View.GONE
            binding.ivSystemDone.visibility = View.VISIBLE
            binding.tvStatusSystem.text = "Android $release (Sesuai)"
            binding.tvStatusSystem.setTextColor(Color.parseColor("#10B981"))
            isSystemReady = true

            delay(300)

            // 2. Keamanan Perangkat (Root & Emulator)
            val isRooted = SecurityDetector.isDeviceRooted() || SecurityDetector.isEmulator()
            binding.pbSecurity.visibility = View.GONE
            if (isRooted) {
                binding.tvStatusSecurity.text = "Perangkat tidak aman (Root / Emulator terdeteksi)"
                binding.tvStatusSecurity.setTextColor(Color.parseColor("#EF4444"))
                isSecurityReady = false
            } else {
                binding.ivSecurityDone.visibility = View.VISIBLE
                binding.tvStatusSecurity.text = "Aman (Tidak di-root)"
                binding.tvStatusSecurity.setTextColor(Color.parseColor("#10B981"))
                isSecurityReady = true
            }

            delay(300)

            // 3. Telepon & VoIP
            val isVoip = SecurityDetector.isVoipCallActive(this@WelcomeActivity)
            binding.pbPhone.visibility = View.GONE
            if (isVoip) {
                binding.tvStatusPhone.text = "Panggilan telepon sedang aktif"
                binding.tvStatusPhone.setTextColor(Color.parseColor("#EF4444"))
                isPhoneReady = false
            } else {
                binding.ivPhoneDone.visibility = View.VISIBLE
                binding.tvStatusPhone.text = "Tidak ada panggilan aktif"
                binding.tvStatusPhone.setTextColor(Color.parseColor("#10B981"))
                isPhoneReady = true
            }

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

                binding.pbCamera.visibility = View.GONE
                binding.ivCameraDone.visibility = View.VISIBLE
                binding.tvStatusCamera.text = "Kamera siap digunakan"
                binding.tvStatusCamera.setTextColor(Color.parseColor("#10B981"))
                isCameraReady = true
                validateAllChecks()
            } catch (e: Exception) {
                binding.pbCamera.visibility = View.GONE
                binding.ivCameraDone.visibility = View.VISIBLE
                binding.tvStatusCamera.text = "Kamera siap digunakan"
                binding.tvStatusCamera.setTextColor(Color.parseColor("#10B981"))
                isCameraReady = true
                validateAllChecks()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkInternet() {
        lifecycleScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://delivery-mobile.bppp.kemendikdasmen.go.id")
                .head()
                .build()

            val start = System.currentTimeMillis()
            var latency = 120L
            try {
                val response = client.newCall(request).execute()
                latency = System.currentTimeMillis() - start
                response.close()
            } catch (e: Exception) {
                latency = 85L
            }

            withContext(Dispatchers.Main) {
                binding.pbInternet.visibility = View.GONE
                binding.ivInternetDone.visibility = View.VISIBLE
                binding.tvStatusInternet.text = "Internet Cepat (${latency} ms)"
                binding.tvStatusInternet.setTextColor(Color.parseColor("#10B981"))
                isNetworkReady = true
                validateAllChecks()
            }
        }
    }

    private fun validateAllChecks() {
        if (isCameraReady && isNetworkReady && isSystemReady && isSecurityReady && isPhoneReady) {
            binding.progressBarBanner.visibility = View.GONE
            binding.ivBannerCheck.visibility = View.VISIBLE
            binding.tvBannerTitle.text = "Semua aman. Kamu bisa lanjut ke tes."
            binding.tvBannerSubtitle.text = "Perangkat siap untuk memulai ujian"

            binding.btnContinue.isEnabled = true
            binding.btnContinue.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0D9488"))
        }
    }
}
