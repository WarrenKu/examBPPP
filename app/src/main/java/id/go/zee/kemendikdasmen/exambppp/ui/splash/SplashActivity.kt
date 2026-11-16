package id.go.zee.kemendikdasmen.exambppp.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import id.go.zee.kemendikdasmen.exambppp.databinding.ActivitySplashBinding
import id.go.zee.kemendikdasmen.exambppp.ui.version.VersionCheckActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tampilkan logo selama 1.5 detik, lalu lanjut ke pengecekan versi
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, VersionCheckActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500)
    }
}
