/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.ImplementUserSource;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversation.message.viewholder.MessageViewHolderManager;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.third.location.viewholder.LocationMessageContentViewHolder;
import cn.wildfire.chat.kit.utils.LogHelper;
import cn.wildfire.chat.kit.utils.Security;
import cn.wildfirechat.chat.BuildConfig;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.push.PushService;
import me.weishu.reflection.Reflection;

public class MyApp extends BaseApp {
    String TAG = getClass().getSimpleName();

    // 一定记得替换为你们自己的，ID请从BUGLY官网申请。关于BUGLY，可以从BUGLY官网了解，或者百度。
    public static String BUGLY_ID = "1";

    @Override
    public void onCreate() {
        super.onCreate();
        AppService.validateConfig(this);
        // bugly，务必替换为你自己的!!!
        if ("wildfirechat.net".equals(Config.IM_SERVER_HOST)) {
            CrashReport.initCrashReport(getApplicationContext(), BUGLY_ID, false);
        }
        // 只在主进程初始化，否则会导致重复收到消息
        if (getCurProcessName(this).equals(BuildConfig.APPLICATION_ID)) {
            // 如果uikit是以aar的方式引入 ，那么需要在此对Config里面的属性进行配置，如：
            // Config.IM_SERVER_HOST = "im.example.com";
            WfcUIKit wfcUIKit = WfcUIKit.getWfcUIKit();
            wfcUIKit.init(this);
            wfcUIKit.setAppServiceProvider(AppService.Instance());
            PushService.init(this, BuildConfig.APPLICATION_ID);
            MessageViewHolderManager.getInstance().registerMessageViewHolder(LocationMessageContentViewHolder.class, R.layout.conversation_item_location_send, R.layout.conversation_item_location_send);
            setupWFCDirs();

            SharedPreferences sp = getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
            String id = sp.getString("id", null);
            String token = sp.getString("token", null);
            if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(token)) {
                //需要注意token跟clientId是强依赖的，一定要调用getClientId获取到clientId，然后用这个clientId获取token，这样connect才能成功，如果随便使用一个clientId获取到的token将无法链接成功。
                //另外不能多次connect，如果需要切换用户请先disconnect，然后3秒钟之后再connect（如果是用户手动登录可以不用等，因为用户操作很难3秒完成，如果程序自动切换请等3秒）
                ChatManagerHolder.gChatManager.connect(id, token);
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 更新图片域名
        AppService.Instance().getImageServiceHost(new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                if (TextUtils.isEmpty(s)) {
                    LogHelper.e(TAG, "getImageServiceHost 没有图片域名");
                    return;
                }
                SharedPreferences sp = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
                String imageHost = sp.getString("imageHost", "");
                if (!imageHost.equals(s)) {
                    sp.edit().putString("imageHost", s).apply();
                }
                ImplementUserSource.Instance().setImageHost(s);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(base, msg, Toast.LENGTH_SHORT).show();
                // 读取 暂存域名
                SharedPreferences sp = getSharedPreferences(Config.SP_INIT_FILE_NAME, Context.MODE_PRIVATE);
                String imageHost = sp.getString("imageHost", "");
                ImplementUserSource.Instance().setImageHost(imageHost);
            }
        });
        Reflection.unseal(base);
    }

    private void setupWFCDirs() {
        Config.VIDEO_SAVE_DIR = this.getDir("video", Context.MODE_PRIVATE).getAbsolutePath();
        Config.AUDIO_SAVE_DIR = this.getDir("audio", Context.MODE_PRIVATE).getAbsolutePath();
        Config.FILE_SAVE_DIR = this.getDir("file", Context.MODE_PRIVATE).getAbsolutePath();

        File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File myPic = new File(sdCard.getPath(), "jChat");
        myPic.mkdirs();

        Config.PHOTO_SAVE_DIR = myPic.getPath();//this.getDir("photo", Context.MODE_PRIVATE).getAbsolutePath();
    }

    public static String getCurProcessName(Context context) {

        int pid = android.os.Process.myPid();

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
                .getRunningAppProcesses()) {

            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }
}
