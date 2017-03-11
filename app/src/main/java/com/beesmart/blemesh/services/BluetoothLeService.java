/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beesmart.blemesh.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	//private String mBluetoothDeviceAddress;
	private static BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	static final int STATE_DISCONNECTED = 0;
	static final int STATE_CONNECTING = 1;
	static final int STATE_CONNECTED = 2;
	
	public final static String ACTION_SCAN_RESULT = "com.quintic.ble.ota.ACTION_SCAN_RESULT";

	public final static String ACTION_GATT_CONNECTED = "com.quintic.ble.ota.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.quintic.ble.ota.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.quintic.ble.ota.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.quintic.ble.ota.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.quintic.ble.ota.EXTRA_DATA";
	public final static String ACTION_WRITE_STATUS = "com.quintic.ble.ota.ACTION_WRITE_STATUS";

	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				mConnectionState = STATE_CONNECTED;
				intentAction = ACTION_GATT_CONNECTED;
				broadcastUpdate(intentAction);
				// Attempts to discover services after successful connection.
				Log.i(TAG,
						"Connected to GATT server and attempting to start service discovery:"
								+ mBluetoothGatt.discoverServices());
				// broadcastUpdate(intentAction);
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
				mConnectionState = STATE_DISCONNECTED;
				/* workaround for bluetooth reconnect,20140722 */
				close();
				Log.i(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			broadcastUpdate(ACTION_WRITE_STATUS, status);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}
	};

	private void broadcastUpdate(final String action, int value) {
		final Intent intent = new Intent(action);
		intent.putExtra(action, value);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
		// For all profiles, writes the data formatted in HEX.
		final byte[] data = characteristic.getValue();
		if (data != null && data.length > 0) {
			intent.putExtra(EXTRA_DATA, data);
		}
		sendBroadcast(intent);
	}
	
	private void broadcastUpdate(final String action, final ScanResult result) {
		final Intent intent = new Intent(action);
		
		intent.putExtra(EXTRA_DATA, result.getDevice().getAddress());
		sendBroadcast(intent);
	}

	public class LocalBinder extends Binder {
		public BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}
		
		mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

		return true;
	}

	private boolean refreshDeviceCache(BluetoothGatt gatt) {
		try {
			BluetoothGatt localBluetoothGatt = gatt;
			Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
			if (localMethod != null) {
				boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
				return bool;
			}
		} catch (Exception localException) {
			Log.e(TAG, "An exception occured while refreshing device");
		}
		return false;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}
		/*
		 * workaround for bluetooth reconnect,20140722 // Previously connected
		 * device. Try to reconnect. if (mBluetoothDeviceAddress != null &&
		 * address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
		 * Log.d(TAG,
		 * "Trying to use an existing mBluetoothGatt for connection."); if
		 * (mBluetoothGatt.connect()) { mConnectionState = STATE_CONNECTING;
		 * return true; } else { return false; } }
		 */
		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		if (mBluetoothGatt == null) {
			Log.i(TAG, "-- start connect --");
			mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
			refreshDeviceCache(mBluetoothGatt);
			//mBluetoothDeviceAddress = address;
			mConnectionState = STATE_CONNECTING;
			Log.d(TAG, "Trying to create a new connection.");
			return true;
		}
		return false;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		
        Log.i(TAG, "-- disconnect --");
        mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}

		mBluetoothGatt.readCharacteristic(characteristic);
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return false;
		}
		return mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
	}

	/**
	 * Request a write on a given {@code BluetoothGattCharacteristic}.
	 * 
	 * @param data
	 *            The data written to device.
	 * @param mBluetoothGatt
	 *            The android system BluetoothGatt handler.
	 */
	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return false;
		}

		characteristic.setValue(data);
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;

		return mBluetoothGatt.getServices();
	}

	public BluetoothGattService getGattService(UUID uuid) {
		if (mBluetoothGatt == null)
			return null;

		return mBluetoothGatt.getService(uuid);
	}

	public boolean getConnectionState() {
		if (mConnectionState == STATE_CONNECTED)
			return true;
		else
			return false;
	}

	public BluetoothGatt getBluetoothGatt() {
		return mBluetoothGatt;
	}
	
	
	private BluetoothLeScanner mBluetoothLeScanner;
	private ScanCallback mScanCallback;
	
	/**
	 * Start scanning for BLE Advertisements (& set it up to stop after a set
	 * period of time).
	 */
	public void startScanning() {

		if (mScanCallback == null) {

			Log.i(TAG, "start a new scan");
			// Start a new scan.
			mScanCallback = new MyScanCallback();
			mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

		} else {
			Log.i(TAG, "start callback is null");
		}
	}

	/**
	 * Stop scanning for BLE Advertisements.
	 */
	public void stopScanning() {

		Log.i(TAG, "stop scan");
		// Stop the scan, remove the callback.
		mBluetoothLeScanner.stopScan(mScanCallback);
		mScanCallback = null;

	}
	
	private List<ScanFilter> buildScanFilters() {
		List<ScanFilter> scanFilters = new ArrayList<>();

		ScanFilter.Builder builder = new ScanFilter.Builder();
		// Comment out the below line to see all BLE devices around you
		scanFilters.add(builder.build());

		return scanFilters;
	}

	private ScanSettings buildScanSettings() {
		ScanSettings.Builder builder = new ScanSettings.Builder();
		builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
		return builder.build();
	}

	private class MyScanCallback extends ScanCallback {

		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			super.onScanResult(callbackType, result);

			broadcastUpdate(ACTION_SCAN_RESULT, result);
		}

		@Override
		public void onScanFailed(int errorCode) {
			super.onScanFailed(errorCode);

		}
	}	
	
}
