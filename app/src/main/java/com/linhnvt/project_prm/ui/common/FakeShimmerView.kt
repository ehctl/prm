package com.linhnvt.project_prm.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.linhnvt.project_prm.R

class FakeShimmerView(context: Context, attr: AttributeSet) : View(context, attr) {
    private var progress = 0F
    private var rect = RectF()
    private var paint = Paint()
    private var blurMask = BlurMaskFilter(50F, BlurMaskFilter.Blur.SOLID)
    private var shimmerAnim : ValueAnimator? = null
    private var loadingBlockWidth = 150
    var animationDuration = 1000L
    var rectRoundedRadius = 10F

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setBackgroundColor(resources.getColor(R.color.shimmer_fake_bg_color))
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawAnim(canvas)
    }

    private fun drawAnim(canvas: Canvas?) {
        val indexX = width * progress
        paint.maskFilter = blurMask
        paint.shader = LinearGradient(
            0F,
            height.toFloat(),
            width.toFloat(),
            height.toFloat(),
            resources.getColor(R.color.shimmer_fake_bg_color),
            resources.getColor(R.color.shimmer_fake_loading_color),
            Shader.TileMode.MIRROR)

        canvas?.drawRoundRect(
            rect.apply {
                top = 0F
                left = indexX
                right = indexX + loadingBlockWidth
                bottom = height.toFloat()
            },
            rectRoundedRadius,
            rectRoundedRadius,
            paint.apply {
                color = resources.getColor(R.color.shimmer_fake_loading_color)
            }
        )
    }

    fun startLoadingAnimation() {
        stopLoadingAnimation()
        shimmerAnim = ValueAnimator.ofFloat(0F,1F).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            duration = animationDuration

            addUpdateListener {
                progress = it.animatedValue as Float
                (this@FakeShimmerView).invalidate()
            }

            start()
        }
    }

    fun stopLoadingAnimation(){
        if( shimmerAnim?.isRunning == true) shimmerAnim?.cancel()
    }
}