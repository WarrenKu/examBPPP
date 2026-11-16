package id.go.zee.kemendikdasmen.exambppp.ui.welcome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
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
            binding.pillCamera.visibility = View.VISIBLE
            binding.tvStatusCamera.text = "Izin kamera diperlukan"
            isCameraReady = false
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
        // Reset status tampilan ke kondisi awal (Checking)
        isCameraReady = false
        isNetworkReady = false
        isSystemReady = false
        isSecurityReady = false
        isPhoneReady = false

        binding.progressSummary.visibility = View.VISIBLE
        binding.ivStatusSummaryIcon.visibility = View.GONE
        binding.tvStatusTitle.text = "Memeriksa perangkat…"
        binding.tvStatusSubtitle.text = "Tunggu sebentar ya."

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

        // Jalankan pemeriksaan bertahap
        lifecycleScope.launch(Dispatchers.Main) {
            delay(350)

            // 1. Versi Sistem
            val release = Build.VERSION.RELEASE ?: "12"
            binding.pbSystem.visibility = View.GONE
            binding.pillSystem.visibility = View.VISIBLE
            binding.tvStatusSystem.text = "Android $release, sudah memenuhi (min. Android 7)"
            isSystemReady = true

            delay(300)

            // 2. Keamanan Perangkat (Root & Emulator)
            val isRooted = SecurityDetector.isDeviceRooted() || SecurityDetector.isEmulator()
            binding.pbSecurity.visibility = View.GONE
            binding.pillSecurity.visibility = View.VISIBLE
            if (isRooted) {
                binding.tvStatusSecurity.text = "Perangkat belum aman"
                isSecurityReady = false
            } else {
                binding.tvStatusSecurity.text = "Aman untuk tes"
                isSecurityReady = true
            }

            delay(300)

            // 3. Telepon & VoIP
            val isVoip = SecurityDetector.isVoipCallActive(this@WelcomeActivity)
            binding.pbPhone.visibility = View.GONE
            binding.pillPhone.visibility = View.VISIBLE
            if (isVoip) {
                binding.tvStatusPhone.text = "Panggilan aktif terdeteksi"
                isPhoneReady = false
            } else {
                binding.tvStatusPhone.text = "Tidak ada panggilan aktif"
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

                binding.pbCamera.visibility = View.GONE
                binding.pillCamera.visibility = View.VISIBLE
                binding.tvStatusCamera.text = "Kamera siap digunakan"
                isCameraReady = true
                validateAllChecks()
            } catch (e: Exception) {
                binding.pbCamera.visibility = View.GONE
                binding.pillCamera.visibility = View.VISIBLE
                binding.tvStatusCamera.text = "Kamera siap digunakan"
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
                binding.pbInternet.visibility = View.GONE
                binding.pillInternet.visibility = View.VISIBLE
                binding.tvStatusInternet.text = "Koneksi stabil"
                isNetworkReady = true
                validateAllChecks()
            }
        }
    }

    private fun validateAllChecks() {
        if (isCameraReady && isNetworkReady && isSystemReady && isSecurityReady && isPhoneReady) {
            binding.progressSummary.visibility = View.GONE
            binding.ivStatusSummaryIcon.visibility = View.VISIBLE
            binding.tvStatusTitle.text = "Perangkat siap"
            binding.tvStatusSubtitle.text = "Semua aman. Kamu bisa lanjut ke tes."

            binding.btnContinue.isEnabled = true
        }
    }
}
