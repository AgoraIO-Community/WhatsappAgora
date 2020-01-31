package com.example.whatsappagora.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.example.whatsappagora.R;
import com.example.whatsappagora.model.VideoInfoData;

public class ViewUtil {
    protected static final boolean DEBUG_ENABLED = false;

    private static final int DEFAULT_TOUCH_TIMESTAMP = -1; // first time

    private static final int TOUCH_COOL_DOWN_TIME = 500; // ms

    private static long mLastTouchTime = DEFAULT_TOUCH_TIMESTAMP;

    /* package */
    static final boolean checkDoubleTouchEvent(MotionEvent event, View view) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) { // only check touch down event
            if (mLastTouchTime == DEFAULT_TOUCH_TIMESTAMP || (SystemClock.elapsedRealtime() - mLastTouchTime) >= TOUCH_COOL_DOWN_TIME) {
                mLastTouchTime = SystemClock.elapsedRealtime();
            } else {
                return true;
            }
        }
        return false;
    }

    /* package */
    static final boolean checkDoubleKeyEvent(KeyEvent event, View view) {

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            if (mLastTouchTime != DEFAULT_TOUCH_TIMESTAMP && (SystemClock.elapsedRealtime() - mLastTouchTime) < TOUCH_COOL_DOWN_TIME) {
                return true;
            }
            mLastTouchTime = SystemClock.elapsedRealtime();
        }

        return false;
    }

    public static void setBackground(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    public static String composeVideoInfoString(Context context, VideoInfoData videoMetaData) {
        // so far do not show delay info
        return videoMetaData.mWidth + "x" + videoMetaData.mHeight + ", "
                + context.getString(R.string.frame_rate_value_with_unit, videoMetaData.mFrameRate) + ", "
                + context.getString(R.string.bit_rate_value_with_unit, videoMetaData.mBitRate);
    }
}
