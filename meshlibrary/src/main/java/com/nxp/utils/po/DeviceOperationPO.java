package com.nxp.utils.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

@Table(name = "deviceOperationTable")
public class DeviceOperationPO
        extends Model {
    @Expose
    @Column(name = "deviceId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private int deviceId;
    @Expose
    @Column(name = "deviceType")
    private int deviceType;
    @Expose
    @Column(name = "deivcePower")
    private int devicedata1;
    @Expose
    @Column(name = "devicedata2")
    private int devicedata2;
    @Expose
    @Column(name = "devicedata3")
    private int devicedata3;
    @Expose
    @Column(name = "devicedata4")
    private int devicedata4;
    @Expose
    @Column(name = "devicedata5")
    private int devicedata5;
    @Expose
    @Column(name = "devicedata6")
    private int devicedata6;
    @Expose
    @Column(name = "devicedata7")
    private int devicedata7;
    @Expose
    @Column(name = "devicedata8")
    private int devicedata8;

    public DeviceOperationPO() {
        super();
    }

    private DeviceOperationPO(int deviceId) {
        this.deviceId = deviceId;
        this.devicedata1 = 0;
        this.devicedata2 = 50;
        this.devicedata3 = 40;
    }

    public int getDeviceId() {
        return this.deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getValue1() {
        return this.devicedata1;
    }

    public void setValue1(int devicedata1) {
        this.devicedata1 = devicedata1;
    }

    public int getValue3() {
        return this.devicedata3;
    }

    public void setValue3(int devicedata3) {
        this.devicedata3 = devicedata3;
    }

    public int getValue2() {
        return this.devicedata2;
    }

    public void setValue2(int devicedata2) {
        this.devicedata2 = devicedata2;
    }

    public void remove() {
        new Delete().from(DeviceOperationPO.class).where("deviceId = ?", new Object[]{Integer.valueOf(this.deviceId)}).execute();
    }

    public static DeviceOperationPO createOperationPO(int deviceId) {
        DeviceOperationPO deviceOperationPO = new DeviceOperationPO(deviceId);
        return deviceOperationPO;
    }

    public static DeviceOperationPO getDeviceOperationPO(int deviceId) {
        return (DeviceOperationPO) new Select().from(DeviceOperationPO.class).where("deviceId = ?", new Object[]{Integer.valueOf(deviceId)}).executeSingle();
    }
}

