package com.example.whatsappagora.utils;

import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.BeautyOptions;

public class Constant {

    public static final String MEDIA_SDK_VERSION;

    static {
        String sdk = "undefined";
        try {
            sdk = RtcEngine.getSdkVersion();
        } catch (Throwable e) {
        }
        MEDIA_SDK_VERSION = sdk;
    }

    public static boolean SHOW_VIDEO_INFO = true;

    public static final int BEAUTY_EFFECT_DEFAULT_CONTRAST = 1;
    public static final float BEAUTY_EFFECT_DEFAULT_LIGHTNESS = .7f;
    public static final float BEAUTY_EFFECT_DEFAULT_SMOOTHNESS = .5f;
    public static final float BEAUTY_EFFECT_DEFAULT_REDNESS = .1f;

}
