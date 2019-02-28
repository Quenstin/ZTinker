package com.example.zhuguoqing.ztinker;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;

import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.app.DefaultApplicationLike;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * 创建作者:zhuguoqing
 * 创建日期:2019/2/28
 * 作用: 集成官方给的application
 */
@DefaultLifeCycle(application = "com.example.zhuguoqing.ztinker.ZTinkerApp",
        //这里填写包名和你想要生成的Application类名，tinker会自动生成该类
        flags = ShareConstants.TINKER_ENABLE_ALL)


public class SampleApplicationLike extends DefaultApplicationLike {


    public SampleApplicationLike(Application application,
                                 int tinkerFlags,
                                 boolean tinkerLoadVerifyFlag,
                                 long applicationStartElapsedTime,
                                 long applicationStartMillisTime,
                                 Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }



    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        
        TinkerInstaller.install(this);
        MultiDex.install(base);

    }
}
