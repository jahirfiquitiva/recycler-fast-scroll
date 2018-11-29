package com.pluscubed.recyclerfastscroll

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.roundToInt

open class RecyclerFastScroller @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    defStyleRes: Int = 0
                                                         ) : FrameLayout(
    context, attrs, defStyleAttr) {
    
    private val mBar: View
    private val mHandle: View?
    private val mHiddenTranslationX: Float
    private val mHide: Runnable
    private val mMinScrollHandleHeight: Int
    private var mOnTouchListener: View.OnTouchListener? = null
    
    internal var mAppBarLayoutOffset: Int = 0
    
    internal var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    internal var mRecyclerView: RecyclerView? = null
    internal var mCoordinatorLayout: CoordinatorLayout? = null
    internal var mAppBarLayout: AppBarLayout? = null
    
    private var mAnimator: AnimatorSet? = null
    internal var mAnimatingIn: Boolean = false
    
    var hideDelay: Int = 0
    private var mHidingEnabled: Boolean = false
    private var mHandleNormalColor: Int = 0
    private var mHandlePressedColor: Int = 0
    private var mBarColor: Int = 0
    private var mTouchTargetWidth: Int = 0
    private var mBarInset: Int = 0
    
    private var mHideOverride: Boolean = false
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private val mAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            requestLayout()
        }
    }
    
    var handlePressedColor: Int
        @ColorInt
        get() = mHandlePressedColor
        set(@ColorInt colorPressed) {
            mHandlePressedColor = colorPressed
            updateHandleColorsAndInset()
        }
    
    var handleNormalColor: Int
        @ColorInt
        get() = mHandleNormalColor
        set(@ColorInt colorNormal) {
            mHandleNormalColor = colorNormal
            updateHandleColorsAndInset()
        }
    
    var barColor: Int
        @ColorInt
        get() = mBarColor
        set(@ColorInt scrollBarColor) {
            mBarColor = scrollBarColor
            updateBarColorAndInset()
        }
    
    var touchTargetWidth: Int
        get() = mTouchTargetWidth
        set(touchTargetWidth) {
            mTouchTargetWidth = touchTargetWidth
            
            val eightDp = 8.dpToPx
            mBarInset = mTouchTargetWidth - eightDp
            
            val fortyEightDp = 48.dpToPx
            if (mTouchTargetWidth > fortyEightDp) {
                throw RuntimeException("Touch target width cannot be larger than 48dp!")
            }
            
            val mBarParams = FrameLayout.LayoutParams(
                touchTargetWidth, ViewGroup.LayoutParams.MATCH_PARENT, GravityCompat.END)
            mBar.layoutParams = mBarParams
            updateViewLayout(mBar, mBarParams)
            
            val mHandleParams = FrameLayout.LayoutParams(
                touchTargetWidth, ViewGroup.LayoutParams.MATCH_PARENT, GravityCompat.END)
            mHandle?.layoutParams = mHandleParams
            updateViewLayout(mHandle, mHandleParams)
            
            updateHandleColorsAndInset()
            updateBarColorAndInset()
            requestLayout()
        }
    
    var isHidingEnabled: Boolean
        get() = mHidingEnabled
        set(hidingEnabled) {
            mHidingEnabled = hidingEnabled
            if (hidingEnabled) {
                postAutoHide()
            }
        }
    
    init {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.RecyclerFastScroller, defStyleAttr, defStyleRes)
        
        mBarColor = a.getColor(
            R.styleable.RecyclerFastScroller_rfs_barColor,
            context.resolveColor(R.attr.colorControlNormal))
        
        mHandleNormalColor = a.getColor(
            R.styleable.RecyclerFastScroller_rfs_handleNormalColor,
            context.resolveColor(R.attr.colorControlNormal))
        
        mHandlePressedColor = a.getColor(
            R.styleable.RecyclerFastScroller_rfs_handlePressedColor,
            context.resolveColor(R.attr.colorAccent))
        
        mTouchTargetWidth = a.getDimensionPixelSize(
            R.styleable.RecyclerFastScroller_rfs_touchTargetWidth, 24.dpToPx)
        
        hideDelay = a.getInt(
            R.styleable.RecyclerFastScroller_rfs_hideDelay, DEFAULT_AUTO_HIDE_DELAY)
        
        mHidingEnabled = a.getBoolean(R.styleable.RecyclerFastScroller_rfs_hidingEnabled, true)
        
        a.recycle()
        
        val fortyEightDp = 48.dpToPx
        layoutParams = ViewGroup.LayoutParams(fortyEightDp, ViewGroup.LayoutParams.MATCH_PARENT)
        
        mBar = View(context)
        mHandle = View(context)
        @Suppress("LeakingThis")
        addView(mBar)
        @Suppress("LeakingThis")
        addView(mHandle)
        
        touchTargetWidth = mTouchTargetWidth
        
        mMinScrollHandleHeight = fortyEightDp
        
        val eightDp = 8.dpToPx
        mHiddenTranslationX =
            (if (context.isRTL) -1.0F else 1.0F) * eightDp
        
        mHide = Runnable {
            if (!mHandle.isPressed) {
                if (mAnimator?.isStarted == true) mAnimator?.cancel()
                mAnimator = AnimatorSet()
                val animator2 = ObjectAnimator.ofFloat(
                    this@RecyclerFastScroller, View.TRANSLATION_X, mHiddenTranslationX)
                animator2.interpolator = FastOutLinearInInterpolator()
                animator2.duration = 150
                mHandle.isEnabled = false
                mAnimator?.play(animator2)
                mAnimator?.start()
            }
        }
        
        mHandle.setOnTouchListener(object : View.OnTouchListener {
            private var mInitialBarHeight: Float = 0.toFloat()
            private var mLastPressedYAdjustedToInitial: Float = 0.toFloat()
            private var mLastAppBarLayoutOffset: Int = 0
            
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                v ?: return false
                event ?: return false
                
                mOnTouchListener?.onTouch(v, event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        mHandle.isPressed = true
                        mSwipeRefreshLayout?.isEnabled = false
                        mRecyclerView?.stopScroll()
                        
                        var nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE
                        nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_VERTICAL
                        
                        mRecyclerView?.startNestedScroll(nestedScrollAxis)
                        
                        mInitialBarHeight = mBar.height.toFloat()
                        mLastPressedYAdjustedToInitial = event.y + mHandle.y + mBar.y
                        mLastAppBarLayoutOffset = mAppBarLayoutOffset
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newHandlePressedY = event.y + mHandle.y + mBar.y
                        val barHeight = mBar.height
                        val newHandlePressedYAdjustedToInitial =
                            newHandlePressedY + (mInitialBarHeight - barHeight)
                        
                        val deltaPressedYFromLastAdjustedToInitial =
                            newHandlePressedYAdjustedToInitial - mLastPressedYAdjustedToInitial
                        
                        val verticalScrollRange = mRecyclerView?.computeVerticalScrollRange() ?: 0
                        val appBarScrollRange = mAppBarLayout?.totalScrollRange ?: 0
                        val dY =
                            (deltaPressedYFromLastAdjustedToInitial / mInitialBarHeight * (verticalScrollRange + appBarScrollRange)).toInt()
                        
                        val params =
                            mAppBarLayout?.layoutParams as? CoordinatorLayout.LayoutParams
                        val behavior = params?.behavior as? AppBarLayout.Behavior
                        if (mCoordinatorLayout != null && mAppBarLayout != null) {
                            behavior?.onNestedPreScroll(
                                mCoordinatorLayout!!, mAppBarLayout!!,
                                this@RecyclerFastScroller, 0, dY, IntArray(2),
                                ViewCompat.TYPE_TOUCH)
                        }
                        
                        updateRvScroll(dY + mLastAppBarLayoutOffset - mAppBarLayoutOffset)
                        
                        mLastPressedYAdjustedToInitial = newHandlePressedYAdjustedToInitial
                        mLastAppBarLayoutOffset = mAppBarLayoutOffset
                    }
                    MotionEvent.ACTION_UP -> {
                        mLastPressedYAdjustedToInitial = -1F
                        mRecyclerView?.stopNestedScroll()
                        mSwipeRefreshLayout?.isEnabled = true
                        mHandle.isPressed = false
                        postAutoHide()
                    }
                }
                
                return true
            }
        })
        
        translationX = mHiddenTranslationX
    }
    
    private fun updateHandleColorsAndInset() {
        val drawable = StateListDrawable()
        
        if (context.isRTL) {
            drawable.addState(
                View.PRESSED_ENABLED_STATE_SET,
                InsetDrawable(ColorDrawable(mHandlePressedColor), 0, 0, mBarInset, 0))
            drawable.addState(
                View.EMPTY_STATE_SET,
                InsetDrawable(ColorDrawable(mHandleNormalColor), 0, 0, mBarInset, 0))
        } else {
            drawable.addState(
                View.PRESSED_ENABLED_STATE_SET,
                InsetDrawable(ColorDrawable(mHandlePressedColor), mBarInset, 0, 0, 0))
            drawable.addState(
                View.EMPTY_STATE_SET,
                InsetDrawable(ColorDrawable(mHandleNormalColor), mBarInset, 0, 0, 0))
        }
        mHandle?.setViewBackground(drawable)
    }
    
    private fun updateBarColorAndInset() {
        val drawable = if (context.isRTL) {
            InsetDrawable(ColorDrawable(mBarColor), 0, 0, mBarInset, 0)
        } else {
            InsetDrawable(ColorDrawable(mBarColor), mBarInset, 0, 0, 0)
        }
        drawable.alpha = 57
        mBar.setViewBackground(drawable)
    }
    
    fun attachSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        mSwipeRefreshLayout = swipeRefreshLayout
    }
    
    fun attachRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
        mRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                this@RecyclerFastScroller.show(true)
            }
        })
        mRecyclerView?.adapter?.let { attachAdapter(it) }
    }
    
    fun attachAdapter(adapter: RecyclerView.Adapter<*>?) {
        if (mAdapter === adapter) return
        mAdapter?.unregisterAdapterDataObserver(mAdapterObserver)
        adapter?.registerAdapterDataObserver(mAdapterObserver)
        mAdapter = adapter
    }
    
    fun attachAppBarLayout(coordinatorLayout: CoordinatorLayout, appBarLayout: AppBarLayout) {
        mCoordinatorLayout = coordinatorLayout
        mAppBarLayout = appBarLayout
        mAppBarLayout?.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                show(true)
                val params = layoutParams as? ViewGroup.MarginLayoutParams
                //AppBarLayout actual height
                params?.topMargin = (mAppBarLayout?.height ?: 0) + verticalOffset
                mAppBarLayoutOffset = -verticalOffset
                layoutParams = params
            })
    }
    
    fun setOnHandleTouchListener(listener: View.OnTouchListener) {
        mOnTouchListener = listener
    }
    
    /**
     * Show the fast scroller and hide after delay
     *
     * @param animate
     * whether to animate showing the scroller
     */
    fun show(animate: Boolean) {
        requestLayout()
        
        post(Runnable {
            if (mHideOverride) {
                return@Runnable
            }
            
            mHandle?.isEnabled = true
            if (animate) {
                if (!mAnimatingIn && translationX != 0f) {
                    if (mAnimator?.isStarted == true) mAnimator?.cancel()
                    mAnimator = AnimatorSet()
                    val animator = ObjectAnimator.ofFloat<View>(
                        this@RecyclerFastScroller, View.TRANSLATION_X, 0F)
                    animator.interpolator = LinearOutSlowInInterpolator()
                    animator.duration = 100
                    animator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            mAnimatingIn = false
                        }
                    })
                    mAnimatingIn = true
                    mAnimator?.play(animator)
                    mAnimator?.start()
                }
            } else {
                translationX = 0f
            }
            postAutoHide()
        })
    }
    
    internal fun postAutoHide() {
        if (mHidingEnabled) {
            mRecyclerView?.removeCallbacks(mHide)
            mRecyclerView?.postDelayed(mHide, hideDelay.toLong())
        }
    }
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mRecyclerView ?: return
        
        val scrollOffset = (mRecyclerView?.computeVerticalScrollOffset() ?: 0) + mAppBarLayoutOffset
        val verticalScrollRange = ((mRecyclerView?.computeVerticalScrollRange() ?: 0) +
            (mAppBarLayout?.totalScrollRange ?: 0) + (mRecyclerView?.paddingBottom ?: 0))
        
        val barHeight = mBar.height
        val ratio = scrollOffset.toFloat() / (verticalScrollRange - barHeight)
        
        var calculatedHandleHeight = (barHeight.toFloat() / verticalScrollRange * barHeight).toInt()
        if (calculatedHandleHeight < mMinScrollHandleHeight) {
            calculatedHandleHeight = mMinScrollHandleHeight
        }
        
        if (calculatedHandleHeight >= barHeight) {
            translationX = mHiddenTranslationX
            mHideOverride = true
            return
        }
        
        mHideOverride = false
        
        val y = (ratio * (barHeight - calculatedHandleHeight)).roundToInt()
        mHandle?.layout(mHandle.left, y, mHandle.right, y + calculatedHandleHeight)
    }
    
    internal fun updateRvScroll(dY: Int) {
        mHandle ?: return
        try {
            mRecyclerView?.scrollBy(0, dY)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
    
    companion object {
        private const val DEFAULT_AUTO_HIDE_DELAY = 1500
    }
}