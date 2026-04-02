package com.example.better_life

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Utility class for reusable UI animations to keep MainActivity clean and professional.
 */
object Animation {

    /**
     * Standard click animation: scales down and back up.
     */
    fun applyClick(view: View, onAnimationEnd: () -> Unit) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction { onAnimationEnd() }
                    .start()
            }
            .start()
    }

    /**
     * Smoothly animates a TextView's numeric value from one number to another.
     */
    fun animateTextValue(textView: TextView?, from: Int, to: Int, duration: Long = 1000) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            textView?.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    /**
     * Smoothly animates a ProgressBar's progress.
     */
    fun animateProgress(progressBar: ProgressBar?, from: Int, to: Int, duration: Long = 1000) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", from, to)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    /**
     * Slide-up and fade-in animation for layout transitions.
     */
    fun animateLayoutEntry(view: View, duration: Long = 450) {
        view.translationY = 80f
        view.alpha = 0f
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * Staggered entry animation for list items.
     */
    fun animateItemEntry(view: View, index: Int, duration: Long = 350) {
        view.alpha = 0f
        view.translationX = 60f
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(duration)
            .setStartDelay(index * 60L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
