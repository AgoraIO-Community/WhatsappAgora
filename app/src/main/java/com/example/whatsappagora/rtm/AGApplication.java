package com.example.whatsappagora.rtm;

import android.app.Application;

public class AGApplication extends Application {
    private static AGApplication sInstance;
    private ChatManager mChatManager;

    public static AGApplication the() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        mChatManager = new ChatManager(this);
        mChatManager.init();
        mChatManager.enableOfflineMessage(true);
    }

    public ChatManager getChatManager() {
        return mChatManager;
    }
}
