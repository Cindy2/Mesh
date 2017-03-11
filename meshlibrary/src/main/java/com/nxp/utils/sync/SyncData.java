package com.nxp.utils.sync;

import com.google.gson.annotations.Expose;

import java.util.List;

import com.nxp.utils.po.DeviceAffiliationPO;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.GroupInfoPO;
import com.nxp.utils.po.SwitchAffiliationPO;


public class SyncData {
    @Expose
    private List<DeviceInfoPO> deviceInfoList;
    @Expose
    private List<GroupInfoPO> groupInfoList;
    @Expose
    private List<DeviceAffiliationPO> deviceAffiliationList;
    @Expose
    private List<SwitchAffiliationPO> switchAffiliationList;

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
}

