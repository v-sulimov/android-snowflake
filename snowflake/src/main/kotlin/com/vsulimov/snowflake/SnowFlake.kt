/*
 * This is the source code from Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright (C) 2021, Nikolai Kudashov, Vitaly Sulimov.
 */

package com.vsulimov.snowflake

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import java.util.Random
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


/**
 * Creates the effect of falling snowflakes.
 * The color and size of snowflakes can be changed via XML attributes.
 */
class SnowFlake : View {

    /**
     * Device display density. This property will be initialized during constructor invocation.
     */
    private val displayDensity: Float

    /**
     * Constant angle diff for thin particle paint.
     */
    private val angleDiff = (Math.PI / 180 * 60).toFloat()

    /**
     * Random number generator that is used to randomly generate coordinates.
     */
    private val random = Random()

    /**
     * Interpolators that are used to generate the alpha channel of snowflakes based on time.
     */
    private val accelerateInterpolator = AccelerateInterpolator()
    private val decelerateInterpolator = DecelerateInterpolator()

    /**
     * Paint that are used to draw particle.
     */
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Paint that are used to draw thin particle.
     */
    private val thinParticlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Color for drawing particles.
     * This value can be changed via the XML attribute sf_snowflakeColor.
     */
    private var particlePaintColor = Color.parseColor(DEFAULT_PARTICLE_COLOR)

    /**
     * Scale multiplier for drawing particles.
     * This value can be changed via the XML attribute sf_snowflakeScaleMultiplier.
     */
    private var particleScaleMultiplier = DEFAULT_PARTICLE_SCALE_MULTIPLIER

    /**
     * Collections with particles.
     */
    private val particles = mutableListOf<Particle>()
    private val freeParticles = mutableListOf<Particle>()

    /**
     * Mutable property that contains the time of the last animation.
     */
    private var lastAnimationTime = 0L

    constructor(context: Context) : this(context = context, attributeSet = null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        displayDensity = context.resources.displayMetrics.density
        attributeSet?.let { consumeAttributeSet(it) }
        configureParticlePaint()
        configureThinParticlePaint()
        generateFreeParticles()
    }

    /**
     * Uses the provided [AttributeSet] and sets the View
     * configuration values ([particlePaintColor], [particleScaleMultiplier])
     * from the resulting [android.content.res.TypedArray] (or sets the default configuration value
     * if one is not present in the [android.content.res.TypedArray]).
     */
    private fun consumeAttributeSet(attributeSet: AttributeSet) {
        val typedArray = context
            .theme
            .obtainStyledAttributes(
                attributeSet,
                R.styleable.SnowFlake,
                DEFAULT_STYLE_ATTRIBUTE,
                DEFAULT_STYLE_RESOURCE
            )
        try {
            particlePaintColor = typedArray.getColor(
                R.styleable.SnowFlake_sf_snowflakeColor,
                Color.parseColor(DEFAULT_PARTICLE_COLOR)
            )
            particleScaleMultiplier = typedArray.getFloat(
                R.styleable.SnowFlake_sf_snowflakeScaleMultiplier,
                DEFAULT_PARTICLE_SCALE_MULTIPLIER
            )
        } finally {
            typedArray.recycle()
        }
    }

    /**
     * Configures [particlePaint].
     */
    private fun configureParticlePaint() {
        particlePaint.strokeWidth = dp(1.5f).toFloat()
        particlePaint.strokeCap = Paint.Cap.ROUND
        particlePaint.style = Paint.Style.STROKE
        particlePaint.color = particlePaintColor
    }

    /**
     * Configures [thinParticlePaint].
     */
    private fun configureThinParticlePaint() {
        thinParticlePaint.strokeWidth = dp(0.5f).toFloat()
        thinParticlePaint.strokeCap = Paint.Cap.ROUND
        thinParticlePaint.style = Paint.Style.STROKE
        thinParticlePaint.color = particlePaintColor
    }

    /**
     * Generates free particles and store it in the [freeParticles] array.
     */
    private fun generateFreeParticles() {
        for (count in 0..19) {
            freeParticles.add(Particle())
        }
    }

    private fun dp(value: Float): Int =
        if (value == 0f) {
            0
        } else ceil((displayDensity * value).toDouble()).toInt()

    private fun dpf2(value: Float): Float =
        if (value == 0f) {
            0f
        } else displayDensity * value

    private inner class Particle {
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var velocity = 0f
        var alpha = 0f
        var lifeTime = 0f
        var currentTime = 0f
        var scale = 0f
        var type = 0
        fun draw(canvas: Canvas) {
            when (type) {
                0 -> {
                    particlePaint.alpha = (255 * alpha).toInt()
                    canvas.drawPoint(x, y, particlePaint)
                }
                1 -> {
                    thinParticlePaint.alpha = (255 * alpha).toInt()
                    var angle = (-Math.PI).toFloat() / 2
                    val px = dpf2(2.0f) * 2 * scale
                    val px1 = -dpf2(0.57f) * 2 * scale
                    val py1 = dpf2(1.55f) * 2 * scale
                    var a = 0
                    while (a < 6) {
                        var x1 = cos(angle.toDouble()).toFloat() * px
                        var y1 = sin(angle.toDouble()).toFloat() * px
                        val cx = x1 * 0.66f
                        val cy = y1 * 0.66f
                        canvas.drawLine(x, y, x + x1, y + y1, thinParticlePaint)
                        val angle2 = (angle - Math.PI / 2).toFloat()
                        x1 = (cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                        y1 = (sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                        canvas.drawLine(x + cx, y + cy, x + x1, y + y1, thinParticlePaint)
                        x1 =
                            (-cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                        y1 =
                            (-sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                        canvas.drawLine(x + cx, y + cy, x + x1, y + y1, thinParticlePaint)
                        angle += angleDiff
                        a++
                    }
                }
                else -> {
                    thinParticlePaint.alpha = (255 * alpha).toInt()
                    var angle = (-Math.PI).toFloat() / 2
                    val px = dpf2(2.0f) * 2 * scale
                    val px1 = -dpf2(0.57f) * 2 * scale
                    val py1 = dpf2(1.55f) * 2 * scale
                    var a = 0
                    while (a < 6) {
                        var x1 = cos(angle.toDouble()).toFloat() * px
                        var y1 = sin(angle.toDouble()).toFloat() * px
                        val cx = x1 * 0.66f
                        val cy = y1 * 0.66f
                        canvas.drawLine(x, y, x + x1, y + y1, thinParticlePaint)
                        val angle2 = (angle - Math.PI / 2).toFloat()
                        x1 = (cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                        y1 = (sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                        canvas.drawLine(x + cx, y + cy, x + x1, y + y1, thinParticlePaint)
                        x1 =
                            (-cos(angle2.toDouble()) * px1 - sin(angle2.toDouble()) * py1).toFloat()
                        y1 =
                            (-sin(angle2.toDouble()) * px1 + cos(angle2.toDouble()) * py1).toFloat()
                        canvas.drawLine(x + cx, y + cy, x + x1, y + y1, thinParticlePaint)
                        angle += angleDiff
                        a++
                    }
                }
            }
        }
    }

    private fun updateParticles(dt: Long) {
        var count = particles.size
        var index = 0
        while (index < count) {
            val particle = particles[index]
            if (particle.currentTime >= particle.lifeTime) {
                if (freeParticles.size < 40) {
                    freeParticles.add(particle)
                }
                particles.removeAt(index)
                index--
                count--
                index++
                continue
            }
            if (particle.currentTime < 200.0f) {
                particle.alpha =
                    accelerateInterpolator.getInterpolation(particle.currentTime / 200.0f)
            } else {
                particle.alpha =
                    1.0f - decelerateInterpolator.getInterpolation((particle.currentTime - 200.0f) / (particle.lifeTime - 200.0f))
            }
            particle.x += particle.vx * particle.velocity * dt / 500.0f
            particle.y += particle.vy * particle.velocity * dt / 500.0f
            particle.currentTime += dt.toFloat()
            index++
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val count = particles.size
        for (index in 0 until count) {
            val particle = particles[index]
            particle.draw(canvas)
        }
        if (random.nextFloat() > 0.7f && particles.size < 100) {
            val cx = random.nextFloat() * measuredWidth
            val cy = random.nextFloat() * (measuredHeight - dp(20f))
            val angle = random.nextInt(40) - 20 + 90
            val vx = cos(Math.PI / 180.0 * angle).toFloat()
            val vy = sin(Math.PI / 180.0 * angle).toFloat()
            val newParticle: Particle
            if (freeParticles.isNotEmpty()) {
                newParticle = freeParticles[0]
                freeParticles.removeAt(0)
            } else {
                newParticle = Particle()
            }
            newParticle.x = cx
            newParticle.y = cy
            newParticle.vx = vx
            newParticle.vy = vy
            newParticle.alpha = 0.0f
            newParticle.currentTime = 0f
            newParticle.scale = random.nextFloat() * particleScaleMultiplier
            newParticle.type = random.nextInt(2)
            newParticle.lifeTime = (2000 + random.nextInt(100)).toFloat()
            newParticle.velocity = 20.0f + random.nextFloat() * 4.0f
            particles.add(newParticle)
        }
        val newTime = System.currentTimeMillis()
        val dt = min(17, newTime - lastAnimationTime)
        updateParticles(dt)
        lastAnimationTime = newTime
        invalidate()
    }

    companion object {

        /**
         * Default style attributes for obtaining TypedArray from AttributeSet.
         */
        private const val DEFAULT_STYLE_ATTRIBUTE = 0
        private const val DEFAULT_STYLE_RESOURCE = 0

        /**
         * Default particle color.
         */
        private const val DEFAULT_PARTICLE_COLOR = "#FFFFFF"

        /**
         * Default particle scale multiplier.
         */
        private const val DEFAULT_PARTICLE_SCALE_MULTIPLIER = 3.2f
    }
}
