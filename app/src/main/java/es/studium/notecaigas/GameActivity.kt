package es.studium.notecaigas

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GameActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var gameView: GameView
    private lateinit var tvTimer: TextView

    private var startTime: Long = 0
    private var elapsedTimeSeconds: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunning = false

    private var baseZChecked = false
    private var initialX = 0f
    private var initialY = 0f

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (timerRunning) {
                val now = System.currentTimeMillis()
                elapsedTimeSeconds = (now - startTime) / 1000
                tvTimer.text = getString(R.string.time_format, elapsedTimeSeconds)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameView = findViewById(R.id.gameView)
        tvTimer = findViewById(R.id.tvTimer)
        val fabExit = findViewById<FloatingActionButton>(R.id.fabExit)

        fabExit.setOnClickListener {
            finish()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        gameView.onGameOverListener = {
            runOnUiThread { showGameOverDialog() }
        }

        gameView.onWinListener = {
            runOnUiThread { showWinDialog() }
        }

        startGameSession()
    }

    private fun startGameSession() {
        baseZChecked = false
        startTime = System.currentTimeMillis()
        elapsedTimeSeconds = 0
        timerRunning = true
        handler.post(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
        gameView.stopGame()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]

            if (!baseZChecked) {
                initialX = x
                initialY = y
                baseZChecked = true
            }

            val tiltX = -(x - initialX)
            val tiltY = (y - initialY)

            gameView.ax = tiltX
            gameView.ay = tiltY
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun showGameOverDialog() {
        timerRunning = false
        handler.removeCallbacks(timerRunnable)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.game_over_title))
            .setMessage(getString(R.string.game_over_message, elapsedTimeSeconds))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_reintentar)) { _, _ ->
                recreate()
            }
            .setNegativeButton(getString(R.string.menu_principal)) { _, _ ->
                finish()
            }
            .show()
    }

    private fun showWinDialog() {
        timerRunning = false
        handler.removeCallbacks(timerRunnable)

        val input = EditText(this).apply {
            hint = getString(R.string.hint_player_name)
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.win_title))
            .setMessage(getString(R.string.win_message, elapsedTimeSeconds))
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
                val name = input.text.toString().trim()
                val playerName = if (name.isEmpty()) getString(R.string.anonymous) else name

                val dbHelper = RankingDbHelper(this)
                dbHelper.addScore(playerName, elapsedTimeSeconds)

                val intent = Intent(this, RankingActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.menu_principal)) { _, _ ->
                val dbHelper = RankingDbHelper(this)
                dbHelper.addScore(getString(R.string.anonymous), elapsedTimeSeconds)
                finish()
            }
            .show()
    }
}
