package com.zrhx.reader.txtreader.shared;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * SP工具，存储设置的属性
 */
public class SetupShared {

    private static final String FONT_SIZE = "font_size";
    private static final String FONT_COLOR = "font_color";
    private static final String SCREEN_LIGHT = "screen_light";
    private static final String BACKGROUND = "background";
    private static final String PAGE_EFFECT = "page_effect";

    private static SharedPreferences sp = null;
    private static Editor editor = null;

    private SetupShared() {

    }

    public static void initSetupShared(Application application) {
        sp = application.getSharedPreferences("SetupShared",
                Context.MODE_PRIVATE);
    }

    /**
     * 保存文本数据的操作
     */
    public static void saveDate(String where, String strData) {
        editor = sp.edit();
        editor.putString(where, strData);
        editor.commit();
    }

    /**
     * 保存数据的操作
     */
    public static void saveDate(String where, int intData) {
        editor = sp.edit();
        editor.putInt(where, intData);
        editor.commit();
    }

    /**
     * 清空存储的数据
     */
    public static void clear() {
        editor = sp.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * 存储字体大小信息
     */
    public static void setFontSize(int fontSize) {
        saveDate(FONT_SIZE, fontSize);
    }

    /**
     * 获取字体大小信息
     * 默认是 18
     */
    public static int getFontSize() {
        return sp.getInt(FONT_SIZE, 18);
    }

    /**
     * 设置字体颜色信息
     */
    public static void setFontColor(int FontColor) {
        saveDate(FONT_COLOR, FontColor);
    }

    /**
     * 获取字体颜色信息
     * 默认是黑色 0xFF000000
     */
    public static int getFontColor() {
        return sp.getInt(FONT_COLOR, 0xFF000000);
    }


    /**
     * 设置屏幕亮度信息
     */
    public static void setSeenLight(int screenLight) {
        saveDate(SCREEN_LIGHT, screenLight);
    }

    /**
     * 获取屏幕亮度信息
     * 默认是50%
     */
    public static int getScreenLight() {
        return sp.getInt(SCREEN_LIGHT, 120);
    }


    /**
     * 设置对应数值背景
     */
    public static void setBackgroundNum(int bgNum) {
        saveDate(BACKGROUND, bgNum);
    }

    /**
     * 获取对应背景的数值
     * 默认是0
     */
    public static int getBackgroundNum() {
        return sp.getInt(BACKGROUND, 0);
    }

    /**
     * 设置对应数值翻页效果
     */
    public static void setEffectNum(int effectNum) {
        saveDate(PAGE_EFFECT, effectNum);
    }

    /**
     * 获取对应翻页效果的数值
     */
    public static int getEffectNum() {
        return sp.getInt(PAGE_EFFECT, 1);
    }

}
