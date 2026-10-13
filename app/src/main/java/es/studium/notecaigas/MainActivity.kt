package es.studium.notecaigas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnRanking = findViewById<Button>(R.id.btnRanking)
        val btnInstructions = findViewById<Button>(R.id.btnInstructions)

        btnPlay.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        btnRanking.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
        }

        btnInstructions.setOnClickListener {
            showInstructionsDialog()
        }
    }

    private fun showInstructionsDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.instructions_title))
            .setMessage(getString(R.string.instructions_text))
            .setPositiveButton(getString(R.string.btn_entendido), null)
            .show()
    }
}
