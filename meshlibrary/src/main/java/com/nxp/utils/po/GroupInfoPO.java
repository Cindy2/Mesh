package com.nxp.utils.po;


import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import com.nxp.utils.log.CSLog;


@Table(name = "groupInfoTable")
public class GroupInfoPO
        extends Model {
    public static final int DELETED_FLAG = 1;
    @Expose
    @Column(name = "groupName")
    private String groupName;
    @Expose
    @Column(name = "groupId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private int groupId;
    @Expose
    @Column(name = "groupFlag")
    private int groupFlag;
    @Expose
    @Column(name = "groupType")
    private int groupType;

    public GroupInfoPO() {
        super();
        this.groupId = 0;
        this.groupFlag = 0;
        this.groupType = 0;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getGroupType() {
        return this.groupType;
    }

    public void setGroupType(int groupType) {
        this.groupType = groupType;
    }


    public void remove() {
        new Delete().from(GroupOperationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).execute();

        new Delete().from(DeviceAffiliationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).execute();

        new Delete().from(SwitchAffiliationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).execute();

        new Delete().from(GroupInfoPO.class).where("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).execute();
    }


    public List<DeviceInfoPO> getDeviceListAffiliations() {
        return
                new Select().from(DeviceInfoPO.class).as("A").innerJoin(DeviceAffiliationPO.class).as("B").on("A.nodeId = B.nodeId").where("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).execute();
    }

    public List<DeviceInfoPO> getDeviceListAffiliationsExceptSwitch() {
        return

                new Select().from(DeviceInfoPO.class).as("A").innerJoin(DeviceAffiliationPO.class).as("B").on("A.nodeId = B.nodeId").where("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).and("A.deviceType <> ?", new Object[]{Integer.valueOf(10)}).execute();
    }

    public boolean containDevice(int deviceId) {
        boolean result = false;
        List<DeviceInfoPO> deviceInfoPOs = getDeviceListAffiliations();

        if ((deviceInfoPOs != null) && (deviceInfoPOs.size() > 0)) {
            for (DeviceInfoPO deviceInfoPO : deviceInfoPOs) {
                if (deviceInfoPO.getNodeId() == deviceId) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    public static GroupInfoPO getGroupInfoPO(int groupId) {
        return
                (GroupInfoPO) new Select().from(GroupInfoPO.class).where("groupId = ?", new Object[]{Integer.valueOf(groupId)}).and("groupFlag <> ?", new Object[]{Integer.valueOf(1)}).executeSingle();
    }

    public static GroupInfoPO createGroupInfoPO() {
        GroupInfoPO groupInfoPO = new GroupInfoPO();
        List<Integer> all = new ArrayList();
        for (int i = 0; i < 255; i++) {
            all.add(Integer.valueOf(i));
        }
        List<GroupInfoPO> groupInfoPOs = getGroupList();
        List<Integer> exist = new ArrayList();
        exist.add(Integer.valueOf(0));
        for (GroupInfoPO group : groupInfoPOs) {
            exist.add(Integer.valueOf(group.getGroupId()));
        }

        boolean valid = all.removeAll(exist);

        if ((valid) && (all.size() > 0)) {
            CSLog.i(GroupInfoPO.class, "all.size() " + all.size());
            int id = ((Integer) all.get((int) (Math.random() * all.size()))).intValue();
            CSLog.i(GroupInfoPO.class, "group id " + id);
            groupInfoPO.setGroupId(id);
        } else {
            return null;
        }


        groupInfoPO.setGroupType(0);

        GroupOperationPO groupOperationPO = GroupOperationPO.getGroupOperationPO(groupInfoPO.getGroupId());
        if (groupOperationPO == null) {
            groupOperationPO = GroupOperationPO.createOperationPO(groupInfoPO.getGroupId());
            groupOperationPO.save();
        }
        return groupInfoPO;
    }

    public static List<GroupInfoPO> getGroupList() {
        return new Select().from(GroupInfoPO.class).where("groupFlag <> ?", new Object[]{Integer.valueOf(1)}).execute();
    }

    public static boolean isGroupNameExist(String groupName) {
        return
                new Select().from(GroupInfoPO.class).where("groupName = ?", new Object[]{groupName}).and("groupFlag <> ?", new Object[]{Integer.valueOf(1)}).exists();
    }
}

