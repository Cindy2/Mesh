package com.beesmart.blemesh.bean;

import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.GroupInfoPO;

import java.util.List;

/**
 * Created by alphawong on 2016/3/30.
 */
public class GroupSection {
    public final static int GROUP_NORMAL = 0;
    public final static int GROUP_ALL = 1;
    public final static int GROUP_SENSOR = 2;
    public final static int GROUP_SWITCH = 3;

    private int mSectionType = 0;
    private GroupInfoPO groupInfoPO;

    private List<DeviceInfoPO> deviceInfoPOs;

    public int getmSectionType() {
        return mSectionType;
    }

    public void setmSectionType(int mSectionType) {
        this.mSectionType = mSectionType;
    }

    public GroupInfoPO getGroupInfoPO() {
        return groupInfoPO;
    }

    public void setGroupInfoPO(GroupInfoPO groupInfoPO) {
        this.groupInfoPO = groupInfoPO;
    }

    public List<DeviceInfoPO> getDeviceInfoPOs() {
        return deviceInfoPOs;
    }

    public void setDeviceInfoPOs(List<DeviceInfoPO> deviceInfoPOs) {
        this.deviceInfoPOs = deviceInfoPOs;
    }
}
