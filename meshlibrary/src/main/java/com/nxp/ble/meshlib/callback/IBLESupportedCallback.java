package com.nxp.ble.meshlib.callback;


public interface IBLESupportedCallback {
    int BLE_UNSUPPORTED = 1;
    int BLE_NOT_ENABLED = 2;
    int BLE_CANNOT_ADVERTISEMENT = 3;

    void onSuccess();

    void onFailed(int statusCode);
}
