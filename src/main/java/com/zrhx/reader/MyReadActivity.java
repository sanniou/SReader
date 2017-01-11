package com.zrhx.reader;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.zrhx.reader.pdfreader.view.FlipperLayout;
import com.zrhx.reader.txtreader.shared.SetupShared;
import com.zrhx.reader.txtreader.view.PageView;

import java.io.IOException;


/**
 * title: 文本显示的界面
 */
public class MyReadActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SetupShared.initSetupShared(getApplication());
//        String filePath = "/sdcard/kotlin-in-chinese.txt";
        String filePath = "/sdcard/adobe.pdf";
        init(filePath);
    }

    private void init(final String filePath) {
        //设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (filePath.endsWith("txt")) {
            PageView mPageView = new PageView(this);
            setContentView(mPageView);
            try {
                mPageView.loadBook(filePath);
            } catch (IOException e) {
                Toast.makeText(this, "电子书不存在", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (filePath.endsWith("pdf")) {
            FlipperLayout layout = new FlipperLayout(this);
            setContentView(layout);
            try {
                layout.loadPdf(filePath);
            } catch (IOException e) {
                Toast.makeText(this, "PDF不存在", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }


}
