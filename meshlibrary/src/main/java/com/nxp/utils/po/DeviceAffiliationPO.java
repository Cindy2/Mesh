package com.nxp.utils.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;


@Table(name = "deviceAffiliationTable")
public class DeviceAffiliationPO
        extends Model {
    @Expose
    @Column(name = "groupId", uniqueGroups = {"group1"}, onUniqueConflicts = {com.activeandroid.annotation.Column.ConflictAction.REPLACE})
    private int groupId;
    @Expose
    @Column(name = "nodeId", uniqueGroups = {"group1"}, onUniqueConflicts = {com.activeandroid.annotation.Column.ConflictAction.REPLACE})
    private int nodeId;

    public DeviceAffiliationPO() {
    }

    public DeviceAffiliationPO(int groupId, int nodeId) {
        this.groupId = groupId;
        this.nodeId = nodeId;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public void remove() {
        new Delete().from(DeviceAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(this.nodeId)}).and("groupId = ?", new Object[]{Integer.valueOf(this.groupId)}).execute();
    }

    public static DeviceAffiliationPO getDeviceAffiliationPO(int groupId, int nodeId) {
        return
                (DeviceAffiliationPO) new Select().from(DeviceAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(nodeId)}).and("groupId = ?", new Object[]{Integer.valueOf(groupId)}).executeSingle();
    }

    public static DeviceAffiliationPO createAffiliationPO(int groupId, int nodeId) {
        DeviceAffiliationPO deviceAffiliationPO = new DeviceAffiliationPO(groupId, nodeId);
        return deviceAffiliationPO;
    }
}

