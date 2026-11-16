package id.go.zee.kemendikdasmen.exambppp.ui.version

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import id.go.zee.kemendikdasmen.exambppp.databinding.ActivityVersionCheckBinding
import id.go.zee.kemendikdasmen.exambppp.ui.welcome.WelcomeActivity

class VersionCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVersionCheckBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVersionCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memeriksa versi selama 1.5 detik lalu lanjut ke layar diagnostik
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500)
    }
}
