package com.beesmart.blemesh.activities;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Button;

import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.UUID;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.sync.MySyncOperations;
import com.beesmart.blemesh.utils.PreferenceUtils;

public class ReceiveDBActivity extends AppCompatActivity {

	private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
	private AdvertiseCallback mAdvertiseCallback;

	private BluetoothGattServer mGattServer;
	private BluetoothGattCharacteristic mServerNotifyCharacteristic;
	private BluetoothDevice mServerNotifyDevice;

	private ProgressDialog mProgressDialog;

	private MySyncOperations mSyncOperations;

	private int totalsize;
	private int currentsize;

	private ByteArrayOutputStream mbyteStream;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_receive);

		mSyncOperations = new MySyncOperations(getApplicationContext());

		mBluetoothLeAdvertiser = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter()
				.getBluetoothLeAdvertiser();

		mProgressDialog = new ProgressDialog(ReceiveDBActivity.this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setProgressNumberFormat("%1d / %2d");
		mProgressDialog.setTitle("Receiving Data");
		mProgressDialog.setMessage("Advertising now");
		mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {

			}
		});

		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

			@Override
			public void onCancel(DialogInterface arg0) {
				if (PreferenceUtils.getPrefInt(ReceiveDBActivity.this, Constants.NETWORK_FLAG,Constants.NETWORK_FLAG_NONE) == Constants.NETWORK_FLAG_JOIN){
					startActivity(new Intent(ReceiveDBActivity.this,MainActivity.class));
				}
				EventBus.getDefault().post(new LocationAffiliationPO());
				finish();
			}
		});

		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.Cancel),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {

						byte[] cmd = { (byte) 0xBB, (byte) 0xBB };
						if (mServerNotifyCharacteristic != null && mServerNotifyDevice != null) {
							mServerNotifyCharacteristic.setValue(cmd);
							mGattServer.notifyCharacteristicChanged(mServerNotifyDevice, mServerNotifyCharacteristic,
									false);
							CSLog.i(ReceiveDBActivity.class, "send notification");
						} else {
							mProgressDialog.cancel();
						}
					}
				});

		mProgressDialog.show();

		startAdvertising();
	}

	protected void onDestroy() {
		super.onDestroy();

		stopAdvertising();

		gattserverclose();
	}

	private void gattserverclose() {

		CSLog.i(ReceiveDBActivity.class, "gattserverclose close");

		if (null != mGattServer) {
			CSLog.i(ReceiveDBActivity.class, "mGattServer close");
			mGattServer.close();
			mGattServer = null;
		}
	}

	private void startAdvertising() {

		CSLog.i(ReceiveDBActivity.class, "startAdvertising start");

		if (mGattServer == null) {
			mGattServer = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).openGattServer(this,
					mGattServerCallback);

			addQPPService();
		}

		if (mAdvertiseCallback == null) {
			AdvertiseSettings settings = buildAdvertiseSettings();
			AdvertiseData data = buildAdvertiseData();
			mAdvertiseCallback = new MyAdvertiseCallback();

			if (mBluetoothLeAdvertiser != null) {
				CSLog.i(ReceiveDBActivity.class, "Service: Starting Advertising");
				mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
			}
		}
	}

	private void stopAdvertising() {

		CSLog.i(ReceiveDBActivity.class, "stopAdvertising start");

		if (mBluetoothLeAdvertiser != null) {
			if (mAdvertiseCallback != null) {
				CSLog.i(ReceiveDBActivity.class, "Service: Stopping Advertising");
				mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
				mAdvertiseCallback = null;
			}
		}
	}

	private AdvertiseData buildAdvertiseData() {

		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		dataBuilder.setIncludeDeviceName(true);

		String key = "";
		try {
			key = PrivateData.getProvisionKey(getApplicationContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
		String uuid16 = PrivateData.createUUID(key);
		String uuid = "0000" + uuid16.toLowerCase(Locale.US) + "-0000-1000-8000-00805f9b34fb";
		dataBuilder.addServiceUuid(ParcelUuid.fromString(uuid));

		// mProgressDialog.setMessage("Advertising with UUID: " + uuid16);

		CSLog.i(ReceiveDBActivity.class, "UUID: " + uuid);
		return dataBuilder.build();
	}

	private AdvertiseSettings buildAdvertiseSettings() {

		AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
		settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
		settingsBuilder.setTimeout(0);
		return settingsBuilder.build();
	}

	private class MyAdvertiseCallback extends AdvertiseCallback {

		@Override
		public void onStartFailure(int errorCode) {
			CSLog.i(ReceiveDBActivity.class, "Advertising failed error = " + errorCode);
		}

		@Override
		public void onStartSuccess(AdvertiseSettings settings) {
			CSLog.i(ReceiveDBActivity.class, "Advertising successfully started");
		}
	}

	private void addQPPService() {

		CSLog.i(ReceiveDBActivity.class, "addQPPService");

		if (null == mGattServer) {
			CSLog.i(ReceiveDBActivity.class, "addQPPService mGattServer is null; return!");
			return;
		}

		BluetoothGattService gattService = mGattServer.getService(UUID.fromString(MeshConstants.QPPS_SERVICE_UUID));

		if (gattService == null) {

			BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
					UUID.fromString(MeshConstants.QPPS_CHAR_WRITE_UUID), BluetoothGattCharacteristic.PROPERTY_WRITE
							| BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
					BluetoothGattCharacteristic.PERMISSION_WRITE);

			BluetoothGattDescriptor writeDescriptor = new BluetoothGattDescriptor(
					UUID.fromString(MeshConstants.OTHER_UUID), BluetoothGattCharacteristic.PERMISSION_READ);

			writeDescriptor.setValue("1.2".getBytes());

			writeCharacteristic.addDescriptor(writeDescriptor);

			BluetoothGattCharacteristic notifyCharacteristic = new BluetoothGattCharacteristic(
					UUID.fromString(MeshConstants.QPPS_CHAR_NOTIFY_UUID), BluetoothGattCharacteristic.PROPERTY_NOTIFY,
					BluetoothGattCharacteristic.PERMISSION_READ);

			BluetoothGattDescriptor notifyDescriptor = new BluetoothGattDescriptor(
					UUID.fromString(MeshConstants.CCCD_UUID), BluetoothGattCharacteristic.PERMISSION_READ
							| BluetoothGattCharacteristic.PERMISSION_WRITE);

			notifyDescriptor.setValue("0".getBytes());

			notifyCharacteristic.addDescriptor(notifyDescriptor);

			BluetoothGattService QPPService = new BluetoothGattService(
					UUID.fromString(MeshConstants.QPPS_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

			QPPService.addCharacteristic(writeCharacteristic);

			QPPService.addCharacteristic(notifyCharacteristic);

			mServerNotifyCharacteristic = notifyCharacteristic;

			mGattServer.addService(QPPService);

		}

	}

	/**
	 * Server Parameters / Slave Server Parameters / Slave
	 */

	private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			super.onConnectionStateChange(device, status, newState);

			CSLog.i(ReceiveDBActivity.class, "server connection new state = " + Integer.toString(newState));
			CSLog.i(ReceiveDBActivity.class, device.getAddress());

			if (newState == BluetoothProfile.STATE_CONNECTED) {

				runOnUiThread(new Runnable() {
					public void run() {
						mProgressDialog.setMessage("Connected");

					}
				});

				CSLog.i(ReceiveDBActivity.class, "BluetoothProfile.STATE_CONNECTED");

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

				mServerNotifyDevice = null;

				CSLog.i(ReceiveDBActivity.class, "BluetoothProfile.STATE_DISCONNECTED");

				// for test
				// stop adv after disconnect accidently
				stopAdvertising();

				// gattserverclose();

				//
				runOnUiThread(new Runnable() {
					public void run() {
						// mProgressDialog.setMessage("Disconnected");

						CSLog.i(ReceiveDBActivity.class, "currentsize " + currentsize + " totalsize " + totalsize);
						if (currentsize != totalsize) {
							mProgressDialog.dismiss();
							createDisconnectDialog();
						} else {
							mProgressDialog.cancel();
						}
					}
				});

			}
		}

		@Override
		public void onServiceAdded(int status, BluetoothGattService service) {
			super.onServiceAdded(status, service);

			CSLog.i(ReceiveDBActivity.class, "onServiceAdded status " + status);
		}

		@Override
		public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
				BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

			CSLog.i(ReceiveDBActivity.class, "onCharacteristicReadRequest");
			CSLog.i(ReceiveDBActivity.class, "characteristic value = " + CryptoUtils.toHex(characteristic.getValue()));

			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());

		}

		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
				BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset,
				byte[] value) {
			super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded,
					offset, value);

			CSLog.i(ReceiveDBActivity.class, "onCharacteristicWriteRequest");
			CSLog.i(ReceiveDBActivity.class, "value = " + CryptoUtils.toHex(value));

			try {
				if (value.length == 8) {
					if (value[0] == (byte) 0xA5 && value[1] == (byte) 0xA5 && value[2] == (byte) 0xA5
							&& value[3] == (byte) 0xA5) {

						totalsize = (int) ((value[4] & 0xFF) | ((value[5] << 8) & 0xFF00)
								| ((value[6] << 16) & 0xFF0000) | ((value[7] << 24) & 0xFF000000));
						currentsize = 0;

						mbyteStream = new ByteArrayOutputStream();

						CSLog.i(ReceiveDBActivity.class, "totalsize = " + totalsize);
					} else {
						mbyteStream.write(value);
						currentsize += value.length;
					}
				} else if (value.length == 4) {
					if (value[0] == (byte) 0x5A && value[1] == (byte) 0x5A && value[2] == (byte) 0x5A
							&& value[3] == (byte) 0x5A) {

						CSLog.i(ReceiveDBActivity.class, "transfer finished");

						// CSLog.i(ReceiveDBActivity.class, new
						// String(mbyteStream.toByteArray()));

						CSLog.i(ReceiveDBActivity.class, "recoveryData finished");

						if (mServerNotifyCharacteristic != null) {
							mServerNotifyCharacteristic.setValue(value);
							mGattServer.notifyCharacteristicChanged(mServerNotifyDevice, mServerNotifyCharacteristic,
									false);
							CSLog.i(ReceiveDBActivity.class, "send notification");
						}

						mSyncOperations.recoveryData(new String(mbyteStream.toByteArray()));

						mbyteStream.close();

						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Button button = ((ProgressDialog) mProgressDialog)
										.getButton(DialogInterface.BUTTON_NEGATIVE);
								button.setText(R.string.OK);
							}
						});
					} else {
						mbyteStream.write(value);
						currentsize += value.length;
					}
				} else {
					mbyteStream.write(value);
					currentsize += value.length;
				}

				// mProgressDialog.setProgress(currentsize);

				float progress = (float) (currentsize * 1.0 / totalsize);
				CSLog.i(ReceiveDBActivity.class, "progress = " + progress + " currentsize = " + currentsize
						+ " totalsize = " + totalsize);
				mProgressDialog.setProgress((int) (progress * 100));

				mProgressDialog.setProgressNumberFormat(String.format("%1d byte / %2d byte", currentsize, totalsize));

				CSLog.i(ReceiveDBActivity.class, "currentsize = " + currentsize);

			} catch (Exception e) {
				e.printStackTrace();
			}

			characteristic.setValue(value);
			if (responseNeeded) {
				mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
						characteristic.getValue());
			}

		}

		@Override
		public void onNotificationSent(BluetoothDevice device, int status) {
			super.onNotificationSent(device, status);

			CSLog.i(ReceiveDBActivity.class, "onNotificationSent status = " + status);
		}

		@Override
		public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
				BluetoothGattDescriptor descriptor) {
			super.onDescriptorReadRequest(device, requestId, offset, descriptor);

			CSLog.i(ReceiveDBActivity.class, "onDescriptorReadRequest");
			CSLog.i(ReceiveDBActivity.class, "value = " + CryptoUtils.toHex(descriptor.getValue()));

			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
		}

		@Override
		public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor,
				boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
			super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

			CSLog.i(ReceiveDBActivity.class, "Our gatt server descriptor was written.");
			CSLog.i(ReceiveDBActivity.class, "device: " + device.getAddress());
			CSLog.i(ReceiveDBActivity.class, "value = " + CryptoUtils.toHex(value));

			mServerNotifyDevice = device;

			if (responseNeeded) {
				mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
			}
		}

		@Override
		public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
			super.onExecuteWrite(device, requestId, execute);

			CSLog.i(ReceiveDBActivity.class, "Our gatt server on execute write.");
		}

	};

	private void createDisconnectDialog() {

		final Builder builder = new AlertDialog.Builder(ReceiveDBActivity.this);
		builder.setTitle("Disconnection");
		builder.setCancelable(false);
		builder.setPositiveButton(R.string.OK, new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

				finish();
			}

		});

		builder.create().show();

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
