package com.beesmart.blemesh.sync;

import com.google.gson.annotations.Expose;
import com.nxp.utils.po.DeviceAffiliationPO;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.GroupInfoPO;
import com.nxp.utils.po.SwitchAffiliationPO;

import java.util.List;

import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.LocationInfoPO;

/**
 * Created by alphawong on 2016/3/29.
 */
public class MySyncData {
    @Expose
    private List<DeviceInfoPO> deviceInfoList;
    @Expose
    private List<GroupInfoPO> groupInfoList;
    @Expose
    private List<DeviceAffiliationPO> deviceAffiliationList;
    @Expose
    private List<SwitchAffiliationPO> switchAffiliationList;
    @Expose
    private List<LocationAffiliationPO> locationAffiliationPOList;
    @Expose
    private List<LocationInfoPO> locationInfoPOList;



    public MySyncData() {
    }

    public List<DeviceInfoPO> getDeviceInfoList() {
        return this.deviceInfoList;
    }

    public List<GroupInfoPO> getGroupInfoList() {
        return this.groupInfoList;
    }

    public List<DeviceAffiliationPO> getDeviceAffiliationList() {
        return this.deviceAffiliationList;
    }

    public List<SwitchAffiliationPO> getSwitchAffiliationList() {
        return this.switchAffiliationList;
    }

    public void setDeviceInfoList(List<DeviceInfoPO> deviceInfoList) {
        this.deviceInfoList = deviceInfoList;
    }

    public void setGroupInfoList(List<GroupInfoPO> groupInfoList) {
        this.groupInfoList = groupInfoList;
    }

    public void setDeviceAffiliationList(List<DeviceAffiliationPO> deviceAffiliationList) {
        this.deviceAffiliationList = deviceAffiliationList;
    }

    public void setSwitchAffiliationList(List<SwitchAffiliationPO> switchAffiliationList) {
        this.switchAffiliationList = switchAffiliationList;
    }

    public List<LocationAffiliationPO> getLocationAffiliationPOList() {
        return locationAffiliationPOList;
    }

    public void setLocationAffiliationPOList(List<LocationAffiliationPO> locationAffiliationPOList) {
        this.locationAffiliationPOList = locationAffiliationPOList;
    }

    public List<LocationInfoPO> getLocationInfoPOList() {
        return locationInfoPOList;
    }

    public void setLocationInfoPOList(List<LocationInfoPO> locationInfoPOList) {
        this.locationInfoPOList = locationInfoPOList;
    }
}
