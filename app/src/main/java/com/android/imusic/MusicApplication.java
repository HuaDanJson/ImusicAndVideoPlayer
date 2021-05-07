package com.android.imusic;

import android.app.Application;
import android.content.Context;
import com.android.imusic.music.activity.MusicPlayerActivity;
import com.android.imusic.music.manager.AppBackgroundManager;
import com.android.imusic.music.manager.ForegroundManager;
import com.android.imusic.net.OkHttpUtils;
import com.music.player.lib.manager.MusicWindowManager;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.tinker.entry.ApplicationLike;
import com.tinkerpatch.sdk.TinkerPatch;
import com.tinkerpatch.sdk.loader.TinkerPatchApplicationLike;

/**
 * 409
 * 2019/3/17
 */

public class MusicApplication extends Application {

    private ApplicationLike mTinkerApplicationLike;
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        initTinker();
        sContext=getApplicationContext();
        ForegroundManager.getInstance().init(this);
        //APP前后台监测,悬浮窗的处理
        AppBackgroundManager.getInstance().setAppStateListener( new AppBackgroundManager.IAppStateChangeListener() {
            @Override
            public void onAppStateChanged(String activityName,boolean isAppForceground) {
                //APP不可见并且直接是从播放器界面不可见的，让悬浮窗显示出来
                if(!isAppForceground&&activityName.equals(MusicPlayerActivity.class.getCanonicalName())){
                    MusicWindowManager.getInstance().onVisible();
                }
//                if(isAppForceground){
//                    MusicWindowManager.getInstance().onVisible();
//                }else{
//                    MusicWindowManager.getInstance().onInvisible();
//                }
            }
        });
        CrashReport.initCrashReport(getApplicationContext(), "da36e5e1da", false);
        if(BuildConfig.FLAVOR.equals("imusicPublish")){
            com.music.player.lib.util.Logger .IS_DEBUG=false;
            com.video.player.lib.utils.Logger.IS_DEBUG=false;
            OkHttpUtils.DEBUG=false;
        }
    }

    /**
     * 初始化热更新
     */
    private void initTinker() {
        // 我们可以从这里获得Tinker加载过程的信息
        if (null==mTinkerApplicationLike) {
            mTinkerApplicationLike = TinkerPatchApplicationLike.getTinkerPatchApplicationLike();
        }
        try {
            // 初始化TinkerPatch SDK, 更多配置可参照API章节中的,初始化SDK
            TinkerPatch.init(mTinkerApplicationLike)
                    .reflectPatchLibrary()
                    .setPatchRollbackOnScreenOff(true)
                    .setPatchRestartOnSrceenOff(true)
                    .setFetchPatchIntervalByHours(1);
            // 每隔3个小时(通过setFetchPatchIntervalByHours设置)去访问后台时候有更新,通过handler实现轮训的效果
            TinkerPatch.with().fetchPatchUpdateAndPollWithInterval();
            TinkerPatch.with().fetchPatchUpdate(true); // 为 true, 每次强制访问服务器更新
        }catch (NoSuchMethodError e){
            e.printStackTrace();
        }catch (RuntimeException e){
            e.printStackTrace();
        }
    }

    public static Context getContext() {
        return sContext;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ForegroundManager.getInstance().onDestroy(this);
        sContext=null;
    }
}