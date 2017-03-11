package com.nxp.ble.meshlib;

import java.util.ArrayList;
import java.util.List;


public class BLEDevice {
    private long timeoutperiod;
    private int nodeid;
    private String macAddress;
    private String deviceName;
    private String setupKey;
    private int rssi;
    private int deviceType;
    private List<Integer> groupsId;
    private int switchControlGroup;
    private int switchControlDevice;

    public BLEDevice() {
        this.timeoutperiod = 10000L;
        this.nodeid = 0;
        this.macAddress = "";
        this.deviceName = "unknown";
        this.setupKey = "";
        this.rssi = -100;
        this.deviceType = 0;
        this.groupsId = new ArrayList();

        this.switchControlGroup = -1;
        this.switchControlDevice = -1;
    }

    public void setTimeoutPeriod(long period) {
        this.timeoutperiod = period;
    }

    public long getTimeoutPeriod() {
        return this.timeoutperiod;
    }


    void setNodeId(int nodeid) {
        this.nodeid = nodeid;
    }


    int getNodeId() {
        return this.nodeid;
    }

    public void setMacAddress(String address) {
        this.macAddress = address;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public void setDeviceName(String name) {
        this.deviceName = name;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setSetupKey(String setupKey) {
        this.setupKey = setupKey;
    }

    public String getSetupKey() {
        return this.setupKey;
    }

    public void setGroupsId(List<Integer> groupsId) {
        this.groupsId = groupsId;
    }

    public List<Integer> getGroupsId() {
        return this.groupsId;
    }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    public int getRSSI() {
        return this.rssi;
    }

    public void setDeviceType(int type) {
        this.deviceType = type;
    }

    public int getDeviceType() {
        return this.deviceType;
    }

    public void setSwitchControlDevice(int deviceId) {
        this.switchControlDevice = deviceId;
    }

    public int getSwitchControlDevice() {
        return this.switchControlDevice;
    }

    public void setSwitchControlGroup(int groupId) {
        this.switchControlGroup = groupId;
    }

    public int getSwitchControlGroup() {
        return this.switchControlGroup;
    }
}
