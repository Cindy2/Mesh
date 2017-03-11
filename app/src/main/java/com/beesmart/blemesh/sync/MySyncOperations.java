package com.beesmart.blemesh.sync;

import android.content.Context;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.LocationInfoPO;

/**
 * Created by alphawong on 2016/3/29.
 */
public class MySyncOperations {
    private String mExtractedFilePath;
    private Context mContext;

    public MySyncOperations(Context context) {
        this.mContext = context;
    }

    public boolean extractData() {
        boolean isSuccess = false;
        Gson gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
        MySyncData syncData = new MySyncData();
        List deviceList = (new Select()).from(DeviceInfoPO.class).execute();
        syncData.setDeviceInfoList(deviceList);
        List groupList = (new Select()).from(GroupInfoPO.class).execute();
        syncData.setGroupInfoList(groupList);
        List deviceAffiliationList = (new Select()).from(DeviceAffiliationPO.class).execute();
        syncData.setDeviceAffiliationList(deviceAffiliationList);
        List switchAffiliationList = (new Select()).from(SwitchAffiliationPO.class).execute();
        syncData.setSwitchAffiliationList(switchAffiliationList);


        List locationInfoList = new Select().from(LocationInfoPO.class).execute();
        syncData.setLocationInfoPOList(locationInfoList);

        List locationInfoAffiliationList = new Select().from(LocationAffiliationPO.class).execute();
        syncData.setLocationAffiliationPOList(locationInfoAffiliationList);

        String jsonStr = gson.toJson(syncData);
        FileWriter writer = null;

        try {
            File e = FileUtils.createFileIntelligently(this.mContext, MySyncConstants.SYNC_DIR, MySyncConstants.SEND_FILE_NAME, (String) null);
            writer = new FileWriter(e);
            writer.write(jsonStr);
            writer.flush();
            this.mExtractedFilePath = e.getAbsolutePath();
            isSuccess = true;
            MediaScanner.scanFile(this.mContext, e);
        } catch (NotAvailableStorageException var15) {
            var15.printStackTrace();
        } catch (IOException var16) {
            var16.printStackTrace();
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
        (new Delete()).from(LocationAffiliationPO.class).execute();
        (new Delete()).from(LocationInfoPO.class).execute();
        Gson gson = (new GsonBuilder()).excludeFieldsWithoutExposeAnnotation().create();
        MySyncData syncData = (MySyncData)gson.fromJson(data, MySyncData.class);
        List deviceInfoPOs = syncData.getDeviceInfoList();
        if(deviceInfoPOs != null) {
            Iterator deviceAffiliationPOs = deviceInfoPOs.iterator();

            while(deviceAffiliationPOs.hasNext()) {
                DeviceInfoPO groupInfoPOs = (DeviceInfoPO)deviceAffiliationPOs.next();
                groupInfoPOs.save();
                CSLog.i(MySyncOperations.class, "id = " + groupInfoPOs.getNodeId());
                DeviceOperationPO switchAffiliationPOs = DeviceOperationPO.getDeviceOperationPO(groupInfoPOs.getNodeId());
                if(switchAffiliationPOs == null) {
                    CSLog.i(MySyncOperations.class, "device createOperationPO = " + groupInfoPOs.getNodeId());
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
                    CSLog.i(MySyncOperations.class, "group createOperationPO = " + deviceAffiliationPOs1.getGroupId());
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

        List<LocationInfoPO> locationInfoPOList = syncData.getLocationInfoPOList();
        if (locationInfoPOList != null){
            Iterator<LocationInfoPO> locationInfoPOIterator = locationInfoPOList.iterator();
            while(locationInfoPOIterator.hasNext()){
                LocationInfoPO locationInfoPO = locationInfoPOIterator.next();
                locationInfoPO.save();
            }
        }

        List<LocationAffiliationPO> locationAffiliationPOList = syncData.getLocationAffiliationPOList();
        if (locationAffiliationPOList != null){
            Iterator<LocationAffiliationPO> iterator = locationAffiliationPOList.iterator();
            while(iterator.hasNext()){
                LocationAffiliationPO locationAffiliationPO = iterator.next();
                locationAffiliationPO.save();
            }
        }
    }
}
