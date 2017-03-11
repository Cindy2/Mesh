package com.nxp.ble.meshlib.callback;


import com.nxp.ble.meshlib.BLEDevice;

public abstract interface IStartScanCallback {
    public static final int SCAN_FAILED_ALREADY_STARTED = 1;
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;
    public static final int SCAN_FAILED_INTERNAL_ERROR = 3;
    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 4;

    public abstract void onTimeout();

    public abstract void onScanResult(BLEDevice paramBLEDevice);

    public abstract void onScanFailed(int paramInt);
}
