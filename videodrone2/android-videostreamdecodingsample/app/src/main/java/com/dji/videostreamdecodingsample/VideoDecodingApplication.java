package com.dji.videostreamdecodingsample;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

public class VideoDecodingApplication extends Application {

    private static BaseProduct mProduct;
    private DJISimulatorApplication simulatorApplication;
    public static synchronized void updateProduct(BaseProduct product) {
        mProduct = product;
    }

    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        com.secneo.sdk.Helper.install(VideoDecodingApplication.this);

        Helper.install(com.dji.videostreamdecodingsample.VideoDecodingApplication.this);
        if (simulatorApplication == null) {
            simulatorApplication = new DJISimulatorApplication();
            simulatorApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        simulatorApplication.onCreate();
    }
}
