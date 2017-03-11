package com.beesmart.blemesh;

import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Delete;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceAffiliationPO;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.DeviceOperationPO;
import com.nxp.utils.po.GroupInfoPO;
import com.nxp.utils.po.GroupOperationPO;
import com.nxp.utils.po.SwitchAffiliationPO;

import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.LocationInfoPO;
import com.beesmart.blemesh.dao.po.SceneAffiliationPO;
import com.beesmart.blemesh.dao.po.SceneDeviceOperationPO;
import com.beesmart.blemesh.dao.po.SceneInfoPO;
import com.qihoo.linker.logcollector.LogCollector;

/**
 * Created by alphawong on 2016/3/1.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        init();

        boolean isDebug = true;
        //set debug mode , you can see debug log , and also you can get logfile in sdcard;
        LogCollector.setDebugMode(isDebug);
        LogCollector.init(getApplicationContext(), "", null);//params can be null
    }


    public boolean isDebug() {
        boolean isDebug = true;
        // isDebug = (this.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        return isDebug;
    }

    private void init() {
        CSLog.isOpen = isDebug();
        initActiveAndroid();
    }

    /**
     * Init ActiveAndroid
     */
    public void initActiveAndroid() {
        ActiveAndroid.initialize(this, isDebug());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
    }

    public static void clearDatabase() {
        new Delete().from(DeviceAffiliationPO.class).execute();
        new Delete().from(DeviceInfoPO.class).execute();
        new Delete().from(GroupInfoPO.class).execute();
        new Delete().from(DeviceOperationPO.class).execute();
        new Delete().from(GroupOperationPO.class).execute();
        new Delete().from(SwitchAffiliationPO.class).execute();
        new Delete().from(LocationInfoPO.class).execute();
        new Delete().from(LocationAffiliationPO.class).execute();
        new Delete().from(SceneInfoPO.class).execute();
        new Delete().from(SceneAffiliationPO.class).execute();
        new Delete().from(SceneDeviceOperationPO.class).execute();
    }

}
