package com.jarvis.app.runtime

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import kotlin.random.Random

class WakeOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        textAlign = Paint.Align.CENTER
    }
    private val matrixPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 190
    }

    private var listeningBreath = 0f
    private var pulseRadius = 0f
    private var lineProgress = 0f
    private var matrixWave = 0f
    private var responseText: String? = null
    private var fullEdgeMode = false

    private val interactionPings = mutableListOf<InteractionPing>()
    private val matrixColumns = MutableList(24) { Random.nextFloat() }

    private var breatheAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var matrixAnimator: ValueAnimator? = null

    fun showWakeListening() {
        responseText = null
        fullEdgeMode = false
        startPulse()
        startBreathing()
        stopMatrix()
        invalidate()
    }

    fun showThinking() {
        responseText = null
        stopBreathing()
        startMatrix()
        invalidate()
    }

    fun showTextResponse(text: String) {
        responseText = text
        stopMatrix()
        startBreathing()
        invalidate()
    }

    fun showControlMode() {
        responseText = null
        fullEdgeMode = true
        stopMatrix()
        startBreathing()
        invalidate()
    }

    fun addTap(x: Float, y: Float) {
        val ping = InteractionPing(x, y, false)
        interactionPings += ping
        invalidate()
    }

    fun addSwipe(x: Float, y: Float) {
        val ping = InteractionPing(x, y, true)
        interactionPings += ping
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val overlayHeight = if (fullEdgeMode) height.toFloat() else height * (2f / 7f)
        val top = height - overlayHeight
        val gradient = LinearGradient(
            0f,
            height.toFloat(),
            0f,
            top,
            Color.argb(220, 0, 0, 0),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        gradientPaint.shader = gradient
        canvas.drawRect(0f, top, width.toFloat(), height.toFloat(), gradientPaint)

        drawEdgeLight(canvas)
        drawThinkingMatrix(canvas, top)
        drawText(canvas, top)
        drawInteractions(canvas)

        if (interactionPings.isNotEmpty()) {
            interactionPings.removeAll { it.alpha <= 0.02f }
            interactionPings.forEach { it.tick() }
            postInvalidateOnAnimation()
        }
    }

    private fun drawEdgeLight(canvas: Canvas) {
        val alpha = (130 + (listeningBreath * 100)).toInt().coerceAtMost(255)
        edgePaint.alpha = alpha
        edgePaint.strokeWidth = if (fullEdgeMode) 4f + listeningBreath * 4f else 8f + listeningBreath * 10f

        val y = height - if (fullEdgeMode) 2f else 6f
        if (lineProgress > 0f && !fullEdgeMode) {
            val half = width * lineProgress / 2f
            canvas.drawLine(width / 2f - half, y, width / 2f + half, y, edgePaint)
        } else if (fullEdgeMode) {
            val inset = edgePaint.strokeWidth
            canvas.drawRect(inset, inset, width - inset, height - inset, edgePaint)
        }

        if (pulseRadius > 0f && !fullEdgeMode) {
            glowPaint.alpha = (255 * (1f - pulseRadius / 80f)).toInt().coerceAtLeast(0)
            canvas.drawCircle(width / 2f, y, pulseRadius, glowPaint)
        }
    }

    private fun drawThinkingMatrix(canvas: Canvas, top: Float) {
        if (matrixAnimator == null) return
        val baseY = top + (height - top) * 0.42f
        val spacing = width / (matrixColumns.size + 1f)
        matrixColumns.forEachIndexed { i, phase ->
            val x = spacing * (i + 1)
            val wave = kotlin.math.sin((matrixWave * 4f) + phase * 6f).toFloat()
            val y = baseY + wave * 32f
            canvas.drawCircle(x, y, 4f + kotlin.math.abs(wave) * 3f, matrixPaint)
        }
    }

    private fun drawText(canvas: Canvas, top: Float) {
        responseText?.let {
            val y = top + (height - top) * 0.52f
            canvas.drawText(it.take(80), width / 2f, y, textPaint)
        }
    }

    private fun drawInteractions(canvas: Canvas) {
        interactionPings.forEach {
            glowPaint.alpha = (255 * it.alpha).toInt().coerceIn(0, 255)
            val radius = if (it.dynamic) 30f else 22f
            canvas.drawCircle(it.x, it.y, radius * (2f - it.alpha), glowPaint)
        }
    }

    private fun startPulse() {
        pulseAnimator?.cancel()
        lineProgress = 0f
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 640
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                pulseRadius = value * 80f
                lineProgress = value
                invalidate()
            }
            doOnEnd {
                pulseRadius = 0f
                invalidate()
            }
            start()
        }
    }

    private fun startBreathing() {
        if (breatheAnimator?.isRunning == true) return
        breatheAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                listeningBreath = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopBreathing() {
        breatheAnimator?.cancel()
        breatheAnimator = null
        listeningBreath = 0f
    }

    private fun startMatrix() {
        matrixAnimator?.cancel()
        matrixAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                matrixWave = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopMatrix() {
        matrixAnimator?.cancel()
        matrixAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        breatheAnimator?.cancel()
        pulseAnimator?.cancel()
        matrixAnimator?.cancel()
    }

    private data class InteractionPing(
        val x: Float,
        val y: Float,
        val dynamic: Boolean,
        var alpha: Float = 1f
    ) {
        fun tick() {
            alpha -= if (dynamic) 0.08f else 0.14f
        }
    }
}
