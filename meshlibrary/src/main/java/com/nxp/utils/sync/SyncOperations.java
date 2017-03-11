package com.nxp.utils.sync;

import android.content.Context;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.nxp.utils.exception.NotAvailableStorageException;
import com.nxp.utils.io.FileUtils;
import com.nxp.utils.io.IOUtils;
import com.nxp.utils.io.MediaScanner;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceAffiliationPO;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.DeviceOperationPO;
import com.nxp.utils.po.GroupInfoPO;
import com.nxp.utils.po.GroupOperationPO;
import com.nxp.utils.po.SwitchAffiliationPO;


public class SyncOperations {
    private String mExtractedFilePath;
    private Context mContext;

    public SyncOperations(Context context) {
        this.mContext = context;
    }

    public boolean extractData() {
        boolean isSuccess = false;
        Gson gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
        SyncData syncData = new SyncData();
        List deviceList = (new Select()).from(DeviceInfoPO.class).execute();
        syncData.setDeviceInfoList(deviceList);
        List groupList = (new Select()).from(GroupInfoPO.class).execute();
        syncData.setGroupInfoList(groupList);
        List deviceAffiliationList = (new Select()).from(DeviceAffiliationPO.class).execute();
        syncData.setDeviceAffiliationList(deviceAffiliationList);
        List switchAffiliationList = (new Select()).from(SwitchAffiliationPO.class).execute();
        syncData.setSwitchAffiliationList(switchAffiliationList);
        String jsonStr = gson.toJson(syncData);
        FileWriter writer = null;

        try {
            File e = FileUtils.createFileIntelligently(this.mContext, SyncConstants.SYNC_DIR, SyncConstants.SEND_FILE_NAME, null);
            writer = new FileWriter(e);
            writer.write(jsonStr);
            writer.flush();
            this.mExtractedFilePath = e.getAbsolutePath();
            isSuccess = true;
            MediaScanner.scanFile(this.mContext, e);
        }  catch (IOException var16) {
            var16.printStackTrace();
        } catch (NotAvailableStorageException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(writer);
        }

        return isSuccess;
    }

    public String getExtractedFilePath() {
        return this.mExtractedFilePath;
    }

    public void recoveryData(String data) {
        (new Delete()).from(DeviceAffiliationPO.class).execute();
        (new Delete()).from(DeviceInfoPO.class).execute();
        (new Delete()).from(GroupInfoPO.class).execute();
        (new Delete()).from(DeviceOperationPO.class).execute();
        (new Delete()).from(GroupOperationPO.class).execute();
        (new Delete()).from(SwitchAffiliationPO.class).execute();
        Gson gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
        SyncData syncData = (SyncData)gson.fromJson(data, SyncData.class);
        List deviceInfoPOs = syncData.getDeviceInfoList();
        if(deviceInfoPOs != null) {
            Iterator deviceAffiliationPOs = deviceInfoPOs.iterator();

            while(deviceAffiliationPOs.hasNext()) {
                DeviceInfoPO groupInfoPOs = (DeviceInfoPO)deviceAffiliationPOs.next();
                groupInfoPOs.save();
                CSLog.i(SyncOperations.class, "id = " + groupInfoPOs.getNodeId());
                DeviceOperationPO switchAffiliationPOs = DeviceOperationPO.getDeviceOperationPO(groupInfoPOs.getNodeId());
                if(switchAffiliationPOs == null) {
                    CSLog.i(SyncOperations.class, "device createOperationPO = " + groupInfoPOs.getNodeId());
                    switchAffiliationPOs = DeviceOperationPO.createOperationPO(groupInfoPOs.getNodeId());
                    switchAffiliationPOs.save();
                }
            }
        }

        List groupInfoPOs1 = syncData.getGroupInfoList();
        if(groupInfoPOs1 != null) {
            Iterator switchAffiliationPOs1 = groupInfoPOs1.iterator();

            while(switchAffiliationPOs1.hasNext()) {
                GroupInfoPO deviceAffiliationPOs1 = (GroupInfoPO)switchAffiliationPOs1.next();
                deviceAffiliationPOs1.save();
                GroupOperationPO switchAffiliationPO = GroupOperationPO.getGroupOperationPO(deviceAffiliationPOs1.getGroupId());
                if(switchAffiliationPO == null) {
                    CSLog.i(SyncOperations.class, "group createOperationPO = " + deviceAffiliationPOs1.getGroupId());
                    switchAffiliationPO = GroupOperationPO.createOperationPO(deviceAffiliationPOs1.getGroupId());
                    switchAffiliationPO.save();
                }
            }
        }

        List deviceAffiliationPOs2 = syncData.getDeviceAffiliationList();
        if(deviceAffiliationPOs2 != null) {
            Iterator switchAffiliationPO1 = deviceAffiliationPOs2.iterator();

            while(switchAffiliationPO1.hasNext()) {
                DeviceAffiliationPO switchAffiliationPOs2 = (DeviceAffiliationPO)switchAffiliationPO1.next();
                switchAffiliationPOs2.save();
            }
        }

        List switchAffiliationPOs3 = syncData.getSwitchAffiliationList();
        if(switchAffiliationPOs3 != null) {
            Iterator var9 = switchAffiliationPOs3.iterator();

            while(var9.hasNext()) {
                SwitchAffiliationPO switchAffiliationPO2 = (SwitchAffiliationPO)var9.next();
                switchAffiliationPO2.save();
            }
        }

    }
}
