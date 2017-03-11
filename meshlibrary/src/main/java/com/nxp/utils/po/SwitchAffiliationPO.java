package com.nxp.utils.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.List;


@Table(name = "switchAffiliationTable")
public class SwitchAffiliationPO
        extends Model {
    @Expose
    @Column(name = "nodeId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private int nodeId;
    @Expose
    @Column(name = "deviceId")
    private int deviceId;
    @Expose
    @Column(name = "groupId")
    private int groupId;

    private SwitchAffiliationPO() {
    }

    private SwitchAffiliationPO(int nodeId, int deviceId, int groupId) {
        this.nodeId = nodeId;
        this.deviceId = deviceId;
        this.groupId = groupId;
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void remove() {
        new Delete().from(SwitchAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(this.nodeId)}).execute();
    }

    public static SwitchAffiliationPO createAffiliationPO(int nodeId, int deviceId, int groupId) {
        SwitchAffiliationPO switchAffiliationPO = new SwitchAffiliationPO(nodeId, deviceId, groupId);
        return switchAffiliationPO;
    }

    public static SwitchAffiliationPO getSwitchAffiliationPO(int nodeId) {
        return (SwitchAffiliationPO) new Select().from(SwitchAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(nodeId)}).executeSingle();
    }

    public static List<SwitchAffiliationPO> getListByControlledDevice(int deviceId) {
        return new Select().from(SwitchAffiliationPO.class).where("deviceId = ?", new Object[]{Integer.valueOf(deviceId)}).execute();
    }

    public static List<SwitchAffiliationPO> getListByControlledGroup(int groupId) {
        return new Select().from(SwitchAffiliationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(groupId)}).execute();
    }

    public static int getControlledDeviceCount(int deviceId) {
        return new Select().from(SwitchAffiliationPO.class).where("deviceId = ?", new Object[]{Integer.valueOf(deviceId)}).count();
    }

    public static int getControlledGroupCount(int groupId) {
        return new Select().from(SwitchAffiliationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(groupId)}).count();
    }
}
