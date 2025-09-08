package com.example.mainquest

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class CustomWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    
    private val rewards = listOf(
        "10 XP",
        "5 XP", 
        "20 XP",
        "Bonus",
        "15 XP",
        "Nothing",
        "25 XP",
        "Lucky!"
    )
    
    private val sectorColors = listOf(
        ContextCompat.getColor(context, R.color.primaryBabyBlue),
        ContextCompat.getColor(context, R.color.secondaryPeachPink),
        ContextCompat.getColor(context, R.color.primaryBabyBlue),
        Color.parseColor("#FFD700"),
        ContextCompat.getColor(context, R.color.secondaryPeachPink),
        Color.parseColor("#808080"),
        ContextCompat.getColor(context, R.color.primaryBabyBlue),
        Color.parseColor("#FF6B6B")
    )
    
    private val sectorAngle = 360f / rewards.size
    
    init {
        textPaint.apply {
            color = ContextCompat.getColor(context, R.color.primaryDarkBlue)
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) - 20f
        
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        
        for (i in rewards.indices) {
            val startAngle = i * sectorAngle - 90f
            
            paint.color = sectorColors[i]
            paint.style = Paint.Style.FILL
            canvas.drawArc(rectF, startAngle, sectorAngle, true, paint)
            
            paint.color = ContextCompat.getColor(context, R.color.primaryDarkBlue)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawArc(rectF, startAngle, sectorAngle, true, paint)
            
            val textAngle = startAngle + sectorAngle / 2f
            val textRadius = radius * 0.7f
            val textX = centerX + textRadius * kotlin.math.cos(Math.toRadians(textAngle.toDouble())).toFloat()
            val textY = centerY + textRadius * kotlin.math.sin(Math.toRadians(textAngle.toDouble())).toFloat()
            
            canvas.save()
            canvas.rotate(textAngle + 90f, textX, textY)
            canvas.drawText(rewards[i], textX, textY + textPaint.textSize / 3f, textPaint)
            canvas.restore()
        }
        
        paint.color = ContextCompat.getColor(context, R.color.primaryDarkBlue)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        paint.color = ContextCompat.getColor(context, R.color.primaryDarkBlue)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, 30f, paint)
        
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(centerX, centerY, 30f, paint)
    }
} 