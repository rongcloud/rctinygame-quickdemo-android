package cn.rongcloud.tinygame;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import java.util.Locale;

import cn.rongcloud.gamelib.api.RCGameEngine;
import cn.rongcloud.gamelib.model.RCGameConfig;
import cn.rongcloud.tinygame.model.UserManager;
import io.rong.imkit.IMCenter;

/**
 * @author gyn
 * @date 2022/3/7
 */
public class MyApplication extends Application {
    // 游戏appId和appKey
    private String appId = "1496435759618818049";
    private String appKey = "YS7NZ6rUAnbi0DruJJiUCmcH1AkCrQk6";

    @Override
    public void onCreate() {
        super.onCreate();
        if (!TextUtils.equals(getProcess(), getPackageName())) {
            return;
        }
        // IMCenter.init(this, "kj7swf8ok3052", false);
        // 初始化im
        IMCenter.init(this, "pvxdm17jpw7ar", false);
        if (UserManager.getUser() != null) {
            // 连接im
            IMCenter.getInstance().connect(UserManager.getUser().getImToken(), null);
        }

        // 初始化游戏
        RCGameConfig gameConfig = RCGameConfig.builder()
                // 设置语言，注意要使用带语言和国家的 Locale, 如：Locale.US 而不是 Locale.ENGLISH, 不支持的语言默认返回英文
                .setGameLanguage(Locale.US)
                // 是否开发模式
                .setDebug(BuildConfig.DEBUG)
                .build();
        RCGameEngine.getInstance().init(this, appId, appKey, gameConfig, null);
    }

    public String getProcess() {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                processName = process.processName;
            }
        }
        return processName;
    }
}
