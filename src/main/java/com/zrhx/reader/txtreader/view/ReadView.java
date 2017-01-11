package com.zrhx.reader.txtreader.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.zrhx.reader.ScreenUtils;
import com.zrhx.reader.txtreader.BackgroundSource;
import com.zrhx.reader.txtreader.service.BookPageService;
import com.zrhx.reader.txtreader.shared.SetupShared;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

public abstract class ReadView extends View {
    private static final String TAG = "ReadView";
    private Bitmap mCurPageBitmap, mNextPageBitmap;//绘制的背景
    private Canvas mCurPageCanvas, mNextPageCanvas;//画板

    private BookPageService mBookPageService;

    private Bitmap mBookBg;//背景

    protected int mWidth;
    protected int mHeight;

    private int mFontSize; //字体大小
    private int mLineSpace = 20;//行间距
    private int mTextColor = Color.BLACK;//字体颜色
    private int mBackColor = 0xffff9e85; // 背景颜色
    private int mMarginWidth = 15; // 左右与边缘的距离
    private int mMarginHeight = 20; // 上下与边缘的距离

    private int mLineCount; // 每页可以显示的行数
    private float mVisibleHeight; // 绘制内容的高
    private float mVisibleWidth; // 绘制内容的宽

    DecimalFormat mFormat = new DecimalFormat("#0.0"); //格式化数字
    private Paint mPaint;
    private int mPercentBottom = 10;


    public ReadView(Context context) {
        this(context, null);
    }

    public ReadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        mWidth = ScreenUtils.getScreenWidth(getContext());
        mHeight = ScreenUtils.getScreenHeight(getContext());

        mFontSize = SetupShared.getFontSize() * 2;
        mTextColor = SetupShared.getFontColor();
        int backgroundNum = SetupShared.getBackgroundNum();
        mBookBg = BitmapFactory.decodeResource(getResources(),
                BackgroundSource.background[backgroundNum]);

        //创建画布
        mCurPageBitmap = Bitmap.createBitmap(mWidth, mHeight,
                Bitmap.Config.ARGB_8888);
        mNextPageBitmap = Bitmap.createBitmap(mWidth, mHeight,
                Bitmap.Config.ARGB_8888);

        mCurPageCanvas = new Canvas(mCurPageBitmap);
        mNextPageCanvas = new Canvas(mNextPageBitmap);

        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setTextSize(mFontSize);
        mPaint.setColor(mTextColor);
        mVisibleWidth = mWidth - mMarginWidth * 2;
        mVisibleHeight = mHeight - mMarginHeight * 2;

        mLineCount = (int) ((mVisibleHeight + mLineSpace) / (mFontSize + mLineSpace)); // 可显示的行数

    }

    public void loadBook(String filePath) throws IOException {
        setBitmaps(mCurPageBitmap, mNextPageBitmap);
        //
        mBookPageService = new BookPageService(mPaint, mVisibleHeight,
                mVisibleWidth, mLineCount);
        mBookPageService.openBook(filePath);
        onDrawPage(mCurPageCanvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            abortAnimation();
            calcCornerXY(event.getX(), event.getY());

            onDrawPage(mCurPageCanvas);
            if (DragToRight()) {
                try {
                    mBookPageService.prePage();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (mBookPageService.isFirstPage()) {
                    Log.e(TAG, "onTouch: 已经是第一页");
                    event.setAction(MotionEvent.ACTION_UP);
                    doTouchEvent(event);
                    return false;
                }
                onDrawPage(mNextPageCanvas);
            } else {
                try {
                    mBookPageService.nextPage();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (mBookPageService.isLastPage()) {
                    Log.e(TAG, "onTouch: 已经是最后一页");
                    event.setAction(MotionEvent.ACTION_UP);
                    doTouchEvent(event);
                    return false;
                }
                onDrawPage(mNextPageCanvas);
            }
            setBitmaps(mCurPageBitmap, mNextPageBitmap);
        }

        ret = doTouchEvent(event);
        return ret;
    }


    //绘制阅读页面
    private void onDrawPage(Canvas c) {
        Vector<String> m_lines = mBookPageService.getTextVector();
        if (m_lines.size() > 0) {
            if (mBookBg == null)
                c.drawColor(mBackColor);
            else
                c.drawBitmap(mBookBg, 0, 0, null);
            int y = mMarginHeight;
            //绘制文本
            for (String strLine : m_lines) {
                y += mFontSize + mLineSpace;
                c.drawText(strLine, mMarginWidth, y, mPaint);
            }
        }
        //绘制进度数字
        float fPercent = mBookPageService.getPercent();
        String strPercent = mFormat.format(fPercent * 100) + "%";
        int nPercentWidth = (int) mPaint.measureText("999.9%") + 1;
        c.drawText(strPercent, mWidth - nPercentWidth, mHeight - mPercentBottom, mPaint);
    }

    /**
     * 处理事件
     */
    protected abstract boolean doTouchEvent(MotionEvent event);

    /**
     * 初始化设置bitmap用于显示文字
     */
    protected abstract void setBitmaps(Bitmap curPageBitmap, Bitmap nextPageBitmap);

    /**
     * 用于page根据落点计算翻页的起点
     */
    protected abstract void calcCornerXY(float x, float y);

    /**
     * 结束翻页动画
     */
    protected abstract void abortAnimation();

    /**
     * @return 是否右划
     */
    protected abstract boolean DragToRight();

}
