package com.zrhx.reader.pdfreader.service;


import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class PdfPageService {
    private ParcelFileDescriptor mFileDescriptor;
    private PdfRenderer mPdfRenderer;
    private int mMaxPage;

    public void openFile(String file) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new IOException("CAN RUNNING ONLY ON API 21 OR HIGHER");
        } else {
            mFileDescriptor = ParcelFileDescriptor.open(new File(file), ParcelFileDescriptor.MODE_READ_ONLY);
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
            mMaxPage = mPdfRenderer.getPageCount();
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void drawPage(Bitmap bitmap, int index) throws IOException {
        Log.e("PdfService", "drawPage: " + index);
        if (index < 0 || index > mMaxPage - 1) {
            return;
        }
        bitmap.eraseColor(Color.WHITE);
        PdfRenderer.Page page = mPdfRenderer.openPage(index);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
    }

    public int getMaxPage() {
        return mMaxPage;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void close() throws IOException {
        if (mPdfRenderer != null) {
            mPdfRenderer.close();
        }
        if (mFileDescriptor != null) {
            mFileDescriptor.close();
        }
    }
}
