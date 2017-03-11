package com.nxp.ble.meshlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.nxp.ble.meshlib.callback.IBLESupportedCallback;


public class BLESupported {
    public static void checkBLEStatus(Context context, IBLESupportedCallback callback) {

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else if (callback != null) {
                    callback.onFailed(3);
                }
            } else if (callback != null) {
                callback.onFailed(2);
            }
        } else if (callback != null) {
            callback.onFailed(1);
        }
    }
}

