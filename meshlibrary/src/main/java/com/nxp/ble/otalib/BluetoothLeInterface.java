package com.nxp.ble.otalib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

/**
 * Created by afan on 2017/2/13.
 */

public abstract class BluetoothLeInterface {
    protected BluetoothGatt mBluetoothGatt = null;
    protected BluetoothGattCharacteristic mWriteCharacteristic = null;
    protected BluetoothGattCharacteristic mReadCharacteristic = null;
    protected BluetoothGattCharacteristic mNotifyCharacteristic = null;

    public BluetoothLeInterface() {
    }

    public boolean bleInterfaceInit() {
        return true;
    }

    public boolean bleInterfaceInit(BluetoothGatt bluetoothGatt) {
        if(bluetoothGatt == null) {
            return false;
        } else {
            this.mBluetoothGatt = bluetoothGatt;
            BluetoothGattService bluetoothGattService = this.mBluetoothGatt.getService(bleGlobalVariables.UUID_QUINTIC_OTA_SERVICE);
            if(bluetoothGattService == null) {
                this.mBluetoothGatt = null;
                return false;
            } else {
                this.mNotifyCharacteristic = bluetoothGattService.getCharacteristic(bleGlobalVariables.UUID_OTA_NOTIFY_CHARACTERISTIC);
                this.mWriteCharacteristic = bluetoothGattService.getCharacteristic(bleGlobalVariables.UUID_OTA_WRITE_CHARACTERISTIC);
                return true;
            }
        }
    }

    public boolean writeCharacteristic(byte[] data) {
        if(this.mBluetoothGatt != null && this.mWriteCharacteristic != null && data != null) {
            this.mWriteCharacteristic.setValue(data);
            return this.mBluetoothGatt.writeCharacteristic(this.mWriteCharacteristic);
        } else {
            return false;
        }
    }

    public boolean readCharacteristic() {
        return this.mBluetoothGatt != null && this.mReadCharacteristic != null?this.mBluetoothGatt.readCharacteristic(this.mReadCharacteristic):false;
    }

    public boolean setCharacteristicNotification(boolean enabled) {
        return this.mBluetoothGatt != null && this.mNotifyCharacteristic != null?this.mBluetoothGatt.setCharacteristicNotification(this.mNotifyCharacteristic, enabled):false;
    }

    public boolean writeDescripter(BluetoothGattDescriptor descriptor, byte[] value) {
        if(this.mBluetoothGatt != null && descriptor != null && value != null) {
            descriptor.setValue(value);
            return this.mBluetoothGatt.writeDescriptor(descriptor);
        } else {
            return false;
        }
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        if(this.mBluetoothGatt != null && characteristic != null && data != null) {
            characteristic.setValue(data);
            return this.mBluetoothGatt.writeCharacteristic(characteristic);
        } else {
            return false;
        }
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        return characteristic == null?false:this.mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        return this.mBluetoothGatt != null && characteristic != null?this.mBluetoothGatt.setCharacteristicNotification(characteristic, enabled):false;
    }

    public boolean writeCharacteristic(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, byte[] data) {
        if(bluetoothGatt != null && characteristic != null && data != null) {
            characteristic.setValue(data);
            return bluetoothGatt.writeCharacteristic(characteristic);
        } else {
            return false;
        }
    }

    public boolean readCharacteristic(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
        return bluetoothGatt != null && characteristic != null?bluetoothGatt.readCharacteristic(characteristic):false;
    }

    public boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, boolean enabled) {
        return bluetoothGatt != null && characteristic != null?bluetoothGatt.setCharacteristicNotification(characteristic, enabled):false;
    }

    public boolean writeDescripter(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor descriptor, byte[] value) {
        if(bluetoothGatt != null && descriptor != null && value != null) {
            descriptor.setValue(value);
            return bluetoothGatt.writeDescriptor(descriptor);
        } else {
            return false;
        }
    }
}
