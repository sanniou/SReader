package com.zrhx.reader.pdfreader.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import com.zrhx.reader.ScreenUtils;
import com.zrhx.reader.pdfreader.service.PdfPageService;

import java.io.IOException;

import uk.co.senab.photoview.PhotoView;

public class FlipperLayout extends ViewGroup {

    private static final String TAG = "FlipperLayout";
    /**
     * 弹性滑动对象，实现过渡效果的滑动
     */
    private Scroller mScroller;

    private VelocityTracker mVelocityTracker;

    private int mVelocityValue = 0;

    /**
     * 商定这个滑动是否有效的距离
     */
    private int limitDistance = 0;

    private int screenWidth = 0;
    private int screenHeight = 0;

    /**
     * 手指移动的方向
     */
    private static final int MOVE_TO_LEFT = 0;
    private static final int MOVE_TO_RIGHT = 1;
    private static final int MOVE_NO_RESULT = 2;

    /**
     * 最后触摸的结果方向
     */
    private int mTouchResult = MOVE_NO_RESULT;
    /**
     * 一开始的方向
     */
    private int mDirection = MOVE_NO_RESULT;

    /**
     * 触摸的模式
     */
    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;

    private int mMode = MODE_NONE;

    /**
     * 滑动的view
     */
    private PhotoView mScrollerView = null;

    /**
     * 最上层的view（处于边缘的，看不到的）
     */
    private PhotoView currentTopView = null;
    /**
     * 显示的view，显示在屏幕
     */
    private PhotoView currentShowView = null;

    /**
     * 最底层的view（看不到的）
     */
    private PhotoView currentBottomView = null;
    private int mMaxPage;
    private int mIndex;
    private PdfPageService mPageService;

    public FlipperLayout(Context context) {
        this(context, null);
    }

    public FlipperLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlipperLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mScroller = new Scroller(context);
        screenWidth = ScreenUtils.getScreenWidth(getContext());
        screenHeight = ScreenUtils.getScreenHeight(getContext());
        mPageService = new PdfPageService();
        this.initFlipperViews(createView(), createView(), createView());
    }

    private PhotoView createView() {
        Bitmap bitmap1 = Bitmap.createBitmap(screenWidth, screenHeight,
                Bitmap.Config.ARGB_8888);
        PhotoView recoverView = new PhotoView(getContext());
        recoverView.setImageBitmap(bitmap1);
        return recoverView;
    }

    /***
     * @param currentBottomView 最底层的view，初始状态看不到
     * @param currentShowView   正在显示的View
     * @param currentTopView    最上层的View，初始化时滑出屏幕
     */
    public void initFlipperViews(PhotoView currentBottomView, PhotoView currentShowView, PhotoView currentTopView) {
        this.currentBottomView = currentBottomView;
        this.currentShowView = currentShowView;
        this.currentTopView = currentTopView;
        addView(currentBottomView);
        addView(currentShowView);
        addView(currentTopView);
        /** 默认将最上层的view滑动到边缘（用于查看上一页） */
        currentTopView.scrollTo(screenWidth, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int height = child.getMeasuredHeight();
            int width = child.getMeasuredWidth();
            child.layout(0, 0, width, height);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    private int startX = 0;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //如果showView 被放大了或者缩小了，不响应翻页
        if (currentShowView.getScale() != 1f) {
            //因为有三个ImageView且最上层为TopView，这里直接分发给ShowView
            return currentShowView.dispatchTouchEvent(event);
        }
        //在View的onTouchEvent方法中追踪当前单击事件的速度
        obtainVelocityTracker(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    break;
                }
                startX = (int) event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                //动画未结束
                if (!mScroller.isFinished()) {
                    return super.onTouchEvent(event);
                }

                if (startX == 0) {
                    startX = (int) event.getX();
                }

                //确定滑动方向
                final int distance = startX - (int) event.getX();
                if (mDirection == MOVE_NO_RESULT) {
                    if (distance > 0 && whetherHasNextPage()) { //左滑
                        mDirection = MOVE_TO_LEFT;
                    } else if (distance < 0 && isFirstPage()) { //右滑
                        mDirection = MOVE_TO_RIGHT;
                    }
                }

                if (mMode == MODE_NONE
                        && ((mDirection == MOVE_TO_LEFT && whetherHasNextPage()) || (mDirection == MOVE_TO_RIGHT
                        && isFirstPage()))) {
                    mMode = MODE_MOVE;
                }

                if (mMode == MODE_MOVE &&
                        (mDirection == MOVE_TO_LEFT && distance <= 0)) {
                    mMode = MODE_NONE;
                }

                if (mDirection != MOVE_NO_RESULT) {
                    //确定滑动的view
                    if (mDirection == MOVE_TO_LEFT) {
                        if (mScrollerView != currentShowView) {
                            mScrollerView = currentShowView;
                        }
                    } else {
                        if (mScrollerView != currentTopView) {
                            mScrollerView = currentTopView;
                        }
                    }

                    if (mMode == MODE_MOVE) {
                        //计算当前的滑动速度 时间间隔1000ms
                        mVelocityTracker.computeCurrentVelocity(1000, ViewConfiguration.getMaximumFlingVelocity());

                        if (mDirection == MOVE_TO_LEFT) {
                            mScrollerView.scrollTo(distance, 0);
                        } else {
                            mScrollerView.scrollTo(screenWidth + distance - startX, 0);
                        }
                        return true;
                    } else {
                        final int scrollX = mScrollerView.getScrollX();

                        if (mDirection == MOVE_TO_LEFT && scrollX != 0 && whetherHasNextPage()) {
                            mScrollerView.scrollTo(0, 0);
                        } else if (mDirection == MOVE_TO_RIGHT && isFirstPage() && screenWidth != Math.abs(scrollX)) {
                            mScrollerView.scrollTo(-screenWidth, 0);
                        }
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                if (mScrollerView == null) {
                    return super.onTouchEvent(event);
                }

                //左正 右负
                final int scrollX = mScrollerView.getScrollX();

                //获取当前的速度
                mVelocityValue = (int) mVelocityTracker.getXVelocity();

                int time = 500;

                if (mMode == MODE_MOVE && mDirection == MOVE_TO_LEFT) {
                    if (scrollX > limitDistance || mVelocityValue < -time) {
                        // 手指向左移动，可以翻屏幕
                        mTouchResult = MOVE_TO_LEFT;
                        if (mVelocityValue < -time) {
                            time = 200;
                        }
                        mScroller.startScroll(scrollX, 0, screenWidth - scrollX, 0, time);
                    } else {
                        mTouchResult = MOVE_NO_RESULT;
                        mScroller.startScroll(scrollX, 0, -scrollX, 0, time);
                    }
                } else if (mMode == MODE_MOVE && mDirection == MOVE_TO_RIGHT) {
                    if ((screenWidth - scrollX) > limitDistance || mVelocityValue > time) {
                        // 手指向右移动，可以翻屏幕
                        mTouchResult = MOVE_TO_RIGHT;
                        if (mVelocityValue > time) {
                            time = 250;
                        }
                        mScroller.startScroll(scrollX, 0, -scrollX, 0, time);
                    } else {
                        mTouchResult = MOVE_NO_RESULT;
                        mScroller.startScroll(scrollX, 0, screenWidth - scrollX, 0, time);
                    }
                }
                resetVariables();
                postInvalidate();
                break;
        }
        return currentShowView.dispatchTouchEvent(event);
    }


    private void resetVariables() {
        mDirection = MOVE_NO_RESULT;
        mMode = MODE_NONE;
        startX = 0;
        releaseVelocityTracker();
    }


    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            mScrollerView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else if (mScroller.isFinished()
                && mTouchResult != MOVE_NO_RESULT) {
            if (mTouchResult == MOVE_TO_LEFT) { //下一页
                mIndex++;
                //改变对象
                currentShowView = currentBottomView;
                currentBottomView = currentTopView;
                currentTopView = mScrollerView;
                //将Bottom放置最底层
                removeView(currentBottomView);
                addView(currentBottomView, 0);
                currentBottomView.scrollTo(0, 0);

                if (!isLastPage()) {
                    updateNext();
                }
            } else { //上一页
                mIndex--;
                //改变对象
                currentTopView = currentBottomView;
                currentBottomView = currentShowView;
                currentShowView = mScrollerView;

                //将Top移到ViewGroup的顶层且左移屏幕距离
                removeView(currentTopView);
                addView(currentTopView);
                currentTopView.scrollTo(screenWidth, 0);


                if (isFirstPage()) {
                    updatePre();
                }
            }
            mTouchResult = MOVE_NO_RESULT;
        }
    }


    private void obtainVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }


    public void loadPdf(String file) throws IOException {
        mPageService.openFile(file);
        mMaxPage = mPageService.getMaxPage();
        mIndex = 0;
        mPageService.drawPage(((BitmapDrawable) currentShowView.getDrawable()).getBitmap(), 0);
        if (mMaxPage > 1) {
            updateNext();
        }
    }

    /***
     * 当前页是否有下一页（用来判断可滑动性）
     */
    public boolean whetherHasNextPage() {
        return !isLastPage();
    }

    /***
     * 当前页是否是最后一页
     */
    public boolean isLastPage() {
        return mIndex == mMaxPage - 1;
    }

    private boolean isFirstPage() {
        return mIndex > 0;
    }

    private void updateNext() {
        try {
            mPageService.drawPage(((BitmapDrawable) currentBottomView.getDrawable()).getBitmap(), mIndex + 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePre() {
        try {
            mPageService.drawPage(((BitmapDrawable) currentTopView.getDrawable()).getBitmap(), mIndex - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDetachedFromWindow() {
        Log.e(TAG, "onDetachedFromWindow And Close File");
        try {
            mPageService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDetachedFromWindow();
    }
}
