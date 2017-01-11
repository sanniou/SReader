package com.zrhx.reader.txtreader.service;

import android.graphics.Paint;
import android.widget.TextView;

import com.zrhx.reader.txtreader.ReadFileUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

/**
 * 读取文件产生页面字符的服务
 */
public class BookPageService {

    private File mBookFile;
    private MappedByteBuffer mMBBuffer;
    private int mMbBufLen = 0; //缓冲区长度
    private int mMbBufBegin = 0;
    private int mMbBufEnd = 0;
    private String mStrCharset = "UTF-8";

    private RandomAccessFile randomAccessFile;
    private Vector<String> m_lines = new Vector<>();

    private int mLineCount;
    private float mHeight;
    private float mWidth;
    private boolean mIsFirstPage = true;
    private boolean mIsLastPage = false;

    private Paint mPaint;

    public BookPageService(TextView mtv) {
        mPaint = mtv.getPaint();
        mWidth = mtv.getWidth();
        mHeight = mtv.getHeight();
        mLineCount = (int) mHeight / mtv.getLineHeight();
    }

    public BookPageService(Paint paint, float height, float width, int lineCount) {
        this.mPaint = paint;
        this.mHeight = height;
        this.mWidth = width;
        this.mLineCount = lineCount;
    }

    /**
     * 读取文件内容到内存
     */
    public void openBook(String strFilePath) throws IOException {
        mBookFile = new File(strFilePath);
        try {
            mStrCharset = ReadFileUtil.getCharset(strFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long lLen = mBookFile.length();
        mMbBufLen = (int) lLen;
        randomAccessFile = new RandomAccessFile(mBookFile, "r");
        mMBBuffer = randomAccessFile.getChannel().map(
                FileChannel.MapMode.READ_ONLY, 0, lLen);
    }

    /**
     * 读取上一段落
     */
    private byte[] readParagraphBack(int nFromPos) {
        int nEnd = nFromPos;
        int i;
        byte b0, b1;
        switch (mStrCharset) {
            case "UTF-16LE":
                i = nEnd - 2;
                while (i > 0) {
                    b0 = mMBBuffer.get(i);
                    b1 = mMBBuffer.get(i + 1);
                    if (b0 == 0x0a && b1 == 0x00 && i != nEnd - 2) {
                        i += 2;
                        break;
                    }
                    i--;
                }

                break;
            case "UTF-16BE":
                i = nEnd - 2;
                while (i > 0) {
                    b0 = mMBBuffer.get(i);
                    b1 = mMBBuffer.get(i + 1);
                    if (b0 == 0x00 && b1 == 0x0a && i != nEnd - 2) {
                        i += 2;
                        break;
                    }
                    i--;
                }
                break;
            default:
                i = nEnd - 1;
                while (i > 0) {
                    b0 = mMBBuffer.get(i);
                    if (b0 == 0x0a && i != nEnd - 1) {
                        i++;
                        break;
                    }
                    i--;
                }
                break;
        }
        if (i < 0)
            i = 0;
        int nParaSize = nEnd - i;
        int j;
        byte[] buf = new byte[nParaSize];
        for (j = 0; j < nParaSize; j++) {
            buf[j] = mMBBuffer.get(i + j);
        }
        return buf;
    }

    /**
     * 读取下一段落
     */
    private byte[] readParagraphForward(int nFromPos) {
        int nStart = nFromPos;
        int i = nStart;
        byte b0, b1;
        // 根据编码格式判断换行
        switch (mStrCharset) {
            case "UTF-16LE":
                while (i < mMbBufLen - 1) {
                    b0 = mMBBuffer.get(i++);
                    b1 = mMBBuffer.get(i++);
                    if (b0 == 0x0a && b1 == 0x00) {
                        break;
                    }
                }
                break;
            case "UTF-16BE":
                while (i < mMbBufLen - 1) {
                    b0 = mMBBuffer.get(i++);
                    b1 = mMBBuffer.get(i++);
                    if (b0 == 0x00 && b1 == 0x0a) {
                        break;
                    }
                }
                break;
            default:
                while (i < mMbBufLen) {
                    b0 = mMBBuffer.get(i++);
                    if (b0 == 0x0a) {
                        break;
                    }
                }
                break;
        }
        int nParaSize = i - nStart;
        byte[] buf = new byte[nParaSize];
        for (i = 0; i < nParaSize; i++) {
            buf[i] = mMBBuffer.get(nFromPos + i);
        }
        return buf;
    }

    /**
     * 获取下一段落的文本行列表
     */
    private Vector<String> getNextPageVector() {
        String strParagraph = "";
        Vector<String> lines = new Vector<>();
        while (lines.size() < mLineCount && mMbBufEnd < mMbBufLen) {
            byte[] paraBuf = readParagraphForward(mMbBufEnd);
            mMbBufEnd += paraBuf.length;
            try {
                strParagraph = new String(paraBuf, mStrCharset);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String strReturn = "";
            if (strParagraph.contains("\r\n")) {
                strReturn = "\r\n";
                strParagraph = strParagraph.replaceAll("\r\n", "");
            } else if (strParagraph.contains("\n")) {
                strReturn = "\n";
                strParagraph = strParagraph.replaceAll("\n", "");
            }

            if (strParagraph.length() == 0) {
                lines.add(strParagraph);
            }
            while (strParagraph.length() > 0) {
                //将一整个段落折断为相应行数的段
                int nSize = mPaint.breakText(strParagraph, true, mWidth, null);
                lines.add(strParagraph.substring(0, nSize));
                strParagraph = strParagraph.substring(nSize);
                if (lines.size() >= mLineCount) {
                    break;
                }
            }
            if (strParagraph.length() != 0) {
                //计算超出屏幕的部分及换行符所占的位数，调整位置指针
                try {
                    mMbBufEnd -= (strParagraph + strReturn)
                            .getBytes(mStrCharset).length;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return lines;
    }

    /**
     * 获取上一段落的文本行列表
     */
    private Vector<String> getPrePageVector() {
        if (mMbBufBegin < 0) {
            mMbBufBegin = 0;
        }
        Vector<String> lines = new Vector<>();
        String strParagraph = "";
        while (lines.size() < mLineCount && mMbBufBegin > 0) {
            Vector<String> paraLines = new Vector<>();
            byte[] paraBuf = readParagraphBack(mMbBufBegin);
            mMbBufBegin -= paraBuf.length;
            try {
                strParagraph = new String(paraBuf, mStrCharset);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            strParagraph = strParagraph.replaceAll("\r\n", "");
            strParagraph = strParagraph.replaceAll("\n", "");

            if (strParagraph.length() == 0) {
                paraLines.add(strParagraph);
            }
            while (strParagraph.length() > 0) {
                int nSize = mPaint.breakText(strParagraph, true, mWidth, null);
                paraLines.add(strParagraph.substring(0, nSize));
                strParagraph = strParagraph.substring(nSize);
            }
            lines.addAll(0, paraLines);
        }
        while (lines.size() > mLineCount) {
            try {
                mMbBufBegin += lines.get(0).getBytes(mStrCharset).length;
                lines.remove(0);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        mMbBufEnd = mMbBufBegin;
        return lines;
    }

    /**
     * 读取上一页的内容
     *
     * @throws IOException
     */
    public void prePage() throws IOException {
        if (mMbBufBegin <= 0) {
            mMbBufBegin = 0;
            mIsFirstPage = true;
            return;
        } else {
            mIsFirstPage = false;
        }
        m_lines.clear();
        getPrePageVector();
        m_lines = getNextPageVector();
    }

    /**
     * 读取下一页的内容
     *
     * @throws IOException
     */
    public void nextPage() throws IOException {
        if (mMbBufEnd >= mMbBufLen) {
            mIsLastPage = true;
            return;
        } else {
            mIsLastPage = false;
        }
        m_lines.clear();
        mMbBufBegin = mMbBufEnd;
        m_lines = getNextPageVector();
    }

    public float getPercent() {
        return (float) (mMbBufBegin * 1.0 / mMbBufLen);
    }

    /**
     * 获取要显示的文本段落
     */
    public String getText() {
        StringBuilder sb = new StringBuilder();
        if (m_lines.size() == 0) {
            m_lines = getNextPageVector();
        }
        if (m_lines.size() > 0) {
            for (String strLine : m_lines) {
                sb.append(strLine).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取要显示的文本行列表
     */
    public Vector<String> getTextVector() {
        if (m_lines.size() == 0) {
            m_lines = getNextPageVector();
        }
        return m_lines;
    }

    public boolean isFirstPage() {
        return mIsFirstPage;
    }

    public boolean isLastPage() {
        return mIsLastPage;
    }
}
