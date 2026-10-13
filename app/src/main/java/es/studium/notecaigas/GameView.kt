package es.studium.notecaigas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Random
import kotlin.math.hypot

data class Hole(val relX: Float, val relY: Float, val relRadius: Float)
data class TargetGoal(val relX: Float, val relY: Float, val relRadius: Float)

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ballPaint = Paint().apply { color = Color.RED; isAntiAlias = true }
    private val holePaint = Paint().apply { color = Color.BLACK; isAntiAlias = true }
    private val holeBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val goalPaint = Paint().apply { color = Color.GREEN; isAntiAlias = true }
    private val goalBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private var ballX = 0f
    private var ballY = 0f
    private var ballRadius = 30f
    private var vx = 0f
    private var vy = 0f

    // Tilt acceleration from sensors
    var ax = 0f
    var ay = 0f

    private var initialized = false
    private var isGameOver = false
    private var isGameWon = false

    private val holes = mutableListOf<Hole>()
    private val goal = TargetGoal(0.5f, 0.1f, 0.08f)

    var onGameOverListener: (() -> Unit)? = null
    var onWinListener: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            updatePhysics()
            invalidate()
            handler.postDelayed(this, 16) // ~60 FPS
        }
    }

    init {
        // Start game loop
        handler.post(runnable)
    }

    fun stopGame() {
        handler.removeCallbacks(runnable)
    }

    fun resetGame() {
        isGameOver = false
        isGameWon = false
        vx = 0f
        vy = 0f
        if (width > 0 && height > 0) {
            ballX = width / 2f
            ballY = height - 100f
            ballRadius = width * 0.035f
        }
        generateRandomHoles()
        handler.post(runnable)
    }

    private fun generateRandomHoles() {
        holes.clear()
        val random = Random()
        val count = 5

        repeat(count) {
            var attempts = 0
            var valid = false
            var rx = 0f
            var ry = 0f
            var rr = 0.05f

            while (!valid && attempts < 50) {
                attempts++
                rx = 0.15f + random.nextFloat() * 0.7f // entre 0.15 y 0.85
                ry = 0.25f + random.nextFloat() * 0.55f // entre 0.25 y 0.80
                rr = 0.04f + random.nextFloat() * 0.02f // radio entre 0.04 y 0.06

                valid = true
                // Distancia a la meta (0.5, 0.1)
                val distToGoal = hypot((rx - goal.relX).toDouble(), (ry - goal.relY).toDouble()).toFloat()
                if (distToGoal < 0.18f) {
                    valid = false
                    continue
                }

                // Distancia a la salida (0.5, inferior)
                val startX = 0.5f
                val startY = 0.85f
                val distToStart = hypot((rx - startX).toDouble(), (ry - startY).toDouble()).toFloat()
                if (distToStart < 0.18f) {
                    valid = false
                    continue
                }

                // Distancia a otros agujeros ya generados
                for (existing in holes) {
                    val dist = hypot((rx - existing.relX).toDouble(), (ry - existing.relY).toDouble()).toFloat()
                    if (dist < 0.15f) {
                        valid = false
                        break
                    }
                }
            }
            holes.add(Hole(rx, ry, rr))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!initialized && w > 0 && h > 0) {
            ballX = w / 2f
            ballY = h - 100f
            ballRadius = w * 0.035f
            generateRandomHoles()
            initialized = true
        }
    }

    private fun updatePhysics() {
        if (!initialized || isGameOver || isGameWon || width == 0 || height == 0) return

        // Update velocity based on tilt acceleration (ax, ay)
        vx += ax * 0.8f
        vy += ay * 0.8f

        // Apply friction/damping
        vx *= 0.95f
        vy *= 0.95f

        // Update position
        ballX += vx
        ballY += vy

        // Wall collisions (bounce and clamp)
        val margin = ballRadius
        if (ballX < margin) {
            ballX = margin
            vx = -vx * 0.3f
        } else if (ballX > width - margin) {
            ballX = width - margin
            vx = -vx * 0.3f
        }

        if (ballY < margin) {
            ballY = margin
            vy = -vy * 0.3f
        } else if (ballY > height - margin) {
            ballY = height - margin
            vy = -vy * 0.3f
        }

        // Check hole collisions first
        for (hole in holes) {
            val holePxX = hole.relX * width
            val holePxY = hole.relY * height
            val holePxRadius = hole.relRadius * width
            val distToHole = hypot((ballX - holePxX).toDouble(), (ballY - holePxY).toDouble()).toFloat()
            if (distToHole < holePxRadius * 0.8f) { // If ball enters hole core
                isGameOver = true
                stopGame()
                invalidate()
                onGameOverListener?.invoke()
                return
            }
        }

        // Check goal collision
        val goalPxX = goal.relX * width
        val goalPxY = goal.relY * height
        val goalPxRadius = goal.relRadius * width
        val distToGoal = hypot((ballX - goalPxX).toDouble(), (ballY - goalPxY).toDouble()).toFloat()
        if (distToGoal < goalPxRadius) {
            isGameWon = true
            stopGame()
            onWinListener?.invoke()
            return
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw goal
        val goalPxX = goal.relX * width
        val goalPxY = goal.relY * height
        val goalPxRadius = goal.relRadius * width
        canvas.drawCircle(goalPxX, goalPxY, goalPxRadius, goalPaint)
        canvas.drawCircle(goalPxX, goalPxY, goalPxRadius, goalBorderPaint)

        // Draw holes
        for (hole in holes) {
            val hx = hole.relX * width
            val hy = hole.relY * height
            val hr = hole.relRadius * width
            canvas.drawCircle(hx, hy, hr, holePaint)
            canvas.drawCircle(hx, hy, hr, holeBorderPaint)
        }

        // Draw red ball only if game is not over (not fallen in hole)
        if (!isGameOver) {
            canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopGame()
    }
}
