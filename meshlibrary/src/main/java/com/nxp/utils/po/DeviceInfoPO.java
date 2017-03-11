package com.nxp.utils.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.List;


@Table(name = "deviceInfoTable")
public class DeviceInfoPO
        extends Model {
    public static final int DELETED_FLAG = 1;
    @Expose
    @Column(name = "deviceNo")
    private String deviceNo;
    @Expose
    @Column(name = "nodeId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private int nodeId;
    @Expose
    @Column(name = "deviceType")
    private int deviceType;
    @Expose
    @Column(name = "deviceName")
    private String deviceName;
    @Expose
    @Column(name = "deviceFlag")
    private int deviceFlag;

    public DeviceInfoPO() {
        super();
        this.nodeId = 0;
        this.deviceType = 0;
        this.deviceFlag = 0;
    }

    public String getDeviceNo() {
        return this.deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }

    public int getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceFlag(int deviceFlag) {
        this.deviceFlag = deviceFlag;
    }

    public int getDeviceFlag() {
        return this.deviceFlag;
    }

    public void remove() {
        new Delete().from(DeviceOperationPO.class).where("deviceId = ?", new Object[]{Integer.valueOf(this.nodeId)}).execute();

        new Delete().from(SwitchAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(this.nodeId)}).execute();


        new Delete().from(DeviceAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(this.nodeId)}).execute();

        setDeviceFlag(1);
        save();
    }

    public void removeAffiliations() {
        new Delete().from(DeviceAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(this.nodeId)}).execute();
    }

    public void removeSwitchAffiliation() {
        new Delete().from(SwitchAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(this.nodeId)}).execute();
    }

    public int[] getGroupAffiliations() {
        int[] groupArray = null;
        List<DeviceAffiliationPO> list = new Select().from(DeviceAffiliationPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(this.nodeId)})
                .orderBy("nodeId ASC").execute();
        if (list != null) {
            int size = list.size();
            groupArray = new int[size];
            int i = 0;
            for (DeviceAffiliationPO aff : list) {
                groupArray[(i++)] = aff.getGroupId();
            }
        }
        return groupArray;
    }

    public static DeviceInfoPO getDeviceInfoPO(String deviceNo) {
        return
                (DeviceInfoPO) new Select().from(DeviceInfoPO.class).where("deviceNo = ?", new Object[]{deviceNo}).and("deviceFlag <> ?", new Object[]{Integer.valueOf(1)}).executeSingle();
    }

    public static DeviceInfoPO getDeviceInfoPO(int deviceId) {
        return
                (DeviceInfoPO) new Select().from(DeviceInfoPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(deviceId)}).and("deviceFlag <> ?", new Object[]{Integer.valueOf(1)}).executeSingle();
    }

    public static DeviceInfoPO getDeleteDeviceInfoPO(int deviceId) {
        return
                (DeviceInfoPO) new Select().from(DeviceInfoPO.class).where("nodeId = ?", new Object[]{Integer.valueOf(deviceId)}).and("deviceFlag = ?", new Object[]{Integer.valueOf(1)}).executeSingle();
    }

    public static DeviceInfoPO createDeviceInfoPO() {
        DeviceInfoPO deviceInfoPO =
                (DeviceInfoPO) new Select().from(DeviceInfoPO.class).where("deviceFlag = ?", new Object[]{Integer.valueOf(1)}).orderBy("nodeId ASC").executeSingle();
        if (deviceInfoPO != null) {
            deviceInfoPO.setDeviceFlag(0);
            deviceInfoPO.setDeviceType(0);
            deviceInfoPO.setDeviceName(null);
            deviceInfoPO.setDeviceNo(null);
        } else {
            deviceInfoPO = new DeviceInfoPO();
            int count = new Select().from(DeviceInfoPO.class).count();
            int id = count + 1;

            if (id > 254) {
                return null;
            }
            deviceInfoPO.setNodeId(id);
        }

        DeviceOperationPO deviceOperationPO = DeviceOperationPO.getDeviceOperationPO(deviceInfoPO.getNodeId());
        if (deviceOperationPO == null) {
            deviceOperationPO = DeviceOperationPO.createOperationPO(deviceInfoPO.getNodeId());
            deviceOperationPO.save();
        }

        return deviceInfoPO;
    }

    public static int getDeviceCount() {
        return
                new Select().from(DeviceInfoPO.class).where("deviceFlag <> ?", new Object[]{Integer.valueOf(1)}).orderBy("deviceType DESC, nodeId ASC").count();
    }

    public static List<DeviceInfoPO> getDeviceList() {
        return
                new Select().from(DeviceInfoPO.class).where("deviceFlag <> ?", new Object[]{Integer.valueOf(1)}).orderBy("deviceType DESC, nodeId ASC").execute();
    }

    public static List<DeviceInfoPO> getDeviceListExceptSwitch() {
        return

                new Select().from(DeviceInfoPO.class).where("deviceFlag <> ?", new Object[]{Integer.valueOf(1)}).and("deviceType <> ?", new Object[]{Integer.valueOf(10)}).orderBy("deviceType DESC, nodeId ASC").execute();
    }

    public static boolean isDeviceNoExist(String deviceNo) {
        return
                new Select().from(DeviceInfoPO.class).where("deviceNo = ?", new Object[]{deviceNo}).and("deviceFlag <> ?", new Object[]{Integer.valueOf(1)}).exists();
    }

}
