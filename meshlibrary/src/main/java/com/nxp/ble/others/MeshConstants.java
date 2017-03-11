package com.nxp.ble.others;

/**
 * Created by afan on 2017/2/13.
 */

public class MeshConstants {
    public static final int CONTROL_FLAG_REQ_BROADCAST = 1;
    public static final int CONTROL_FLAG_REQ_NEXT_SLOT = 2;
    public static final int CONTROL_FLAG_RESP_BROADCAST = 4;
    public static final int CONTROL_FLAG_RESP_NEXT_SLOT = 8;
    public static final int DEVICE_TYPE_LED = 1;
    public static final int DEVICE_TYPE_BUZZER = 2;
    public static final int DEVICE_TYPE_RGB_BULB = 3;
    public static final int DEVICE_TYPE_CCTW_BULB = 4;
    public static final int DEVICE_TYPE_SWITCH = 10;
    public static final int DEVICE_TYPE_TEMP = 11;
    public static final int DEVICE_TYPE_HUMI = 12;
    public static final int DEVICE_TYPE_TEMINAL = 13;
    public static final String[] DEVICE_NAME = new String[]{"NONE", "LED", "Buzzer", "RGB Bulb", "CCTW Bulb", "NONE", "NONE", "NONE", "NONE", "NONE", "Switch", "Tempeture", "Humidity", "Teminal"};
    public static final String QPPS_SERVICE_UUID = "0000fee9-0000-1000-8000-00805f9b34fb";
    public static final String QPPS_CHAR_WRITE_UUID = "d44bc439-abfd-45a2-b575-925416129600";
    public static final String QPPS_CHAR_NOTIFY_UUID = "d44bc439-abfd-45a2-b575-925416129601";
    public static final String CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String OTHER_UUID = "00002901-0000-1000-8000-00805f9b34fb";
    public static String OTA_FILE_BASE = "MESH_OTA";
    public static String LOG_FILE_BASE = "MyLog";
}
