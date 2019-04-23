package com.example.hancher.greatcamera;

import android.app.Application;
import android.content.Context;

/**
 * Created by liaohaicong on 2019/4/23.
 */

public class GreatApp extends Application {

    private static  Context instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static  Context getAppContext() {
        return instance;
    }
}
