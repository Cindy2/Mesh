package com.beesmart.blemesh.activities;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.io.FileUtils;
import com.nxp.utils.log.CSLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.adapter.ScanResultAdapter;
import com.beesmart.blemesh.bean.ScanResultDevice;
import com.beesmart.blemesh.sync.MySyncConstants;
import com.beesmart.blemesh.sync.MySyncOperations;

public class SendDBActivity extends AppCompatActivity {

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothLeScanner mBluetoothLeScanner;
	private ScanCallback mScanCallback;

	private static BluetoothGatt mBluetoothGatt;
	private BluetoothGattService mQPPService;
	private BluetoothGattCharacteristic mQPPCharacteristic;
	private BluetoothGattCharacteristic mNotifyCharacteristic;

	private ListView mScanResultListView;
	private ScanResultAdapter mScanResultAdapter;
//	private ScanDeviceAdapter mScanResultAdapter;

	private ProgressDialog mProgressDialog;
	private ProgressAsyncTask mAsyncTask;
	private MySyncOperations mSyncOperations;

	private int totalsize;
	private int currentsize;

	private int mConnectStatus = STATE_DISCONNECTED;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	private static final int STATE_DISCONNECTED = 3;

	private int mSendStatus = SEND_HEADER;
	private static final int SEND_HEADER = 1;
	private static final int SEND_BODY = 2;
	// private static final int SEND_FINISH = 3;

	private String mUUID;
	private String mSelectedAddress;

	private boolean mScanning = false;

	private Handler mScanStopHandler;
	private Runnable mScanStopRunnable = new Runnable() {
		@Override
		public void run() {
			// timeout and stop scan
			CSLog.i(SendDBActivity.class, "scan timeout and stop scan");

			if (mScanning) {
				mBluetoothLeScanner.stopScan(mScanCallback);
				mScanCallback = null;
				mScanning = false;
				invalidateOptionsMenu();
			}

		}
	};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_send);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//		setTitle("");
		mSyncOperations = new MySyncOperations(getApplicationContext());

		mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
		mBluetoothLeScanner = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter()
				.getBluetoothLeScanner();

		mScanStopHandler = new Handler();

		mScanResultListView = (ListView) findViewById(R.id.listview_sendscan);
		mScanResultAdapter = new ScanResultAdapter(this);
//		mScanResultAdapter = new ScanDeviceAdapter(this,new ArrayList<BLEDevice>());


		mScanResultListView.setAdapter(mScanResultAdapter);

		mScanResultListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				stopScanning();
				mScanning = false;
				invalidateOptionsMenu();

				mProgressDialog = new ProgressDialog(SendDBActivity.this);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDialog.setCancelable(false);
				mProgressDialog.setCanceledOnTouchOutside(false);
				mProgressDialog.setTitle("Send Data");
				mProgressDialog.setMessage("Prepare data");
				mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface arg0) {
						// for test
						gattclose();
					}
				});

				mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

					@Override
					public void onCancel(DialogInterface arg0) {
						finish();
					}
				});

				mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {

						if (mAsyncTask != null) {
							mAsyncTask.cancel(true);
						}

						disconnect();
					}
				});

				mProgressDialog.show();

				boolean valid = mSyncOperations.extractData();

				if (valid) {
					mSelectedAddress = ((ScanResultDevice) mScanResultAdapter.getItem(position)).address;
					mConnectStatus = STATE_CONNECTING;
					connect(mSelectedAddress);
				} else {
					Toast.makeText(SendDBActivity.this, "Cannot make database file!", Toast.LENGTH_SHORT).show();
				}
			}

		});

		mScanning = false;
		invalidateOptionsMenu();

		// clean list
		if (mScanResultAdapter != null) {
			mScanResultAdapter.clear();
			mScanResultAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return super.onSupportNavigateUp();

	}

	protected void onDestroy() {
		super.onDestroy();

		if (mScanning) {
			mBluetoothLeScanner.stopScan(mScanCallback);
			mScanCallback = null;
			mScanning = false;
			invalidateOptionsMenu();
		}
	}

//	private class ScanResultAdapter extends BaseAdapter {
//
//		private LayoutInflater mInflater;
//
//		private ArrayList<ScanResultDevice> mScanResults;
//
//		public ScanResultAdapter() {
//			mInflater = LayoutInflater.from(getApplicationContext());
//
//			mScanResults = new ArrayList<ScanResultDevice>();
//		}
//
//		public void addDevice(ScanResultDevice result) {
//
//			boolean isalready = false;
//			for (ScanResultDevice scanResult : mScanResults) {
//				if (scanResult.address.equals(result.address)) {
//					scanResult.rssi = result.rssi;
//					isalready = true;
//					break;
//				}
//			}
//			if (!isalready) {
//				mScanResults.add(result);
//			}
//		}
//
//		public void clear() {
//			mScanResults.clear();
//		}
//
//		@Override
//		public int getCount() {
//			return mScanResults.size();
//		}
//
//		@Override
//		public Object getItem(int i) {
//			return mScanResults.get(i);
//		}
//
//		@Override
//		public long getItemId(int i) {
//			return i;
//		}
//
//		@Override
//		public View getView(int i, View view, ViewGroup parent) {
//
//			ScanResultViewHolder viewHolder;
//			if (view == null) {
//				view = mInflater.inflate(R.layout.listview_provision, parent, false);
//				viewHolder = new ScanResultViewHolder();
//				viewHolder.deviceAddress = (TextView) view.findViewById(R.id.text_device_address);
//				viewHolder.deviceName = (TextView) view.findViewById(R.id.text_device_name);
//				viewHolder.rssi = (TextView) view.findViewById(R.id.text_rssi);
//				viewHolder.typeIcon = (ImageView) view.findViewById(R.id.icon_device_type);
//				viewHolder.signal = (ImageView) view.findViewById(R.id.image_rssi);
//				view.setTag(viewHolder);
//			} else {
//				viewHolder = (ScanResultViewHolder) view.getTag();
//			}
//
//			ScanResultDevice result = mScanResults.get(i);
//			viewHolder.deviceName.setText(result.name);
//			viewHolder.deviceAddress.setText(result.address);
//			viewHolder.rssi.setText("RSSI: " + result.rssi + "db");
//			viewHolder.typeIcon.setBackgroundResource(R.drawable.ic_type_bluetooth);
//			viewHolder.signal.setImageLevel(100 + result.rssi);
//
//			return view;
//		}
//	}

//	static class ScanResultViewHolder {
//		TextView deviceName;
//		TextView deviceAddress;
//		TextView rssi;
//		ImageView typeIcon;
//		ImageView signal;
//	}
//
//	static class ScanResultDevice {
//		String name;
//		String address;
//		int rssi;
//	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.menu_add_device, menu);

		if (!mScanning) {
			menu.findItem(R.id.action_stop).setVisible(false);
			menu.findItem(R.id.action_scan).setVisible(true);
//			menu.findItem(R.id.a).setActionView(null);
		} else {
			menu.findItem(R.id.action_stop).setVisible(true);
			menu.findItem(R.id.action_scan).setVisible(false);
//			menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {

		case R.id.action_scan:

			if (mScanCallback == null) {
				CSLog.i(SendDBActivity.class, "Starting Scanning");

				mScanning = true;
				invalidateOptionsMenu();

				mScanResultAdapter.clear();
				mScanResultAdapter.notifyDataSetChanged();

				try {
					String key = PrivateData.getProvisionKey(getApplicationContext());

					if (key != null) {
						mUUID = PrivateData.createUUID(key);
						startScanning();
					} else {
						// Toast.makeText(SendDBActivity.this, "There is no setup key!", Toast.LENGTH_SHORT).show();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				CSLog.i(SendDBActivity.class, "already in scanning");
			}

			break;

		case R.id.action_stop:

			CSLog.i(SendDBActivity.class, "Stopping Scanning");

			mScanning = false;
			invalidateOptionsMenu();

			stopScanning();
			break;

		case android.R.id.home:
			onBackPressed();
			return true;

		}

		return true;
	}

	public void startScanning() {

		if (mScanCallback == null) {
			CSLog.i(SendDBActivity.class, "Starting Scanning");

			// Will stop the scanning after a set time.
			mScanStopHandler.postDelayed(mScanStopRunnable, 10000);

			// Start a new scan.
			mScanCallback = new MyScanCallback();
			mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);

		} else {
			CSLog.i(SendDBActivity.class, "already in scanning");
		}
	}

	public void stopScanning() {
		CSLog.i(SendDBActivity.class, "Stopping Scanning");

		// Stop the scan, remove the callback.
		mBluetoothLeScanner.stopScan(mScanCallback);
		mScanCallback = null;

		mScanStopHandler.removeCallbacks(mScanStopRunnable);
	}

	private class MyScanCallback extends ScanCallback {

		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			super.onScanResult(callbackType, result);

			boolean includeuuid = false;
			List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
			if (parcelUuids != null) {
				for (ParcelUuid parcelUuid : parcelUuids) {
					CSLog.i(SendDBActivity.class, "uuid: " + parcelUuid.toString());
					if (parcelUuid.toString().toUpperCase(Locale.US).contains(mUUID)) {
						includeuuid = true;
						break;
					}
				}
			}

			if (includeuuid && result.getRssi() > -80) {
				ScanResultDevice scanResultDevice = new ScanResultDevice();
				scanResultDevice.address = result.getDevice().getAddress();
				scanResultDevice.name = (result.getDevice().getName() == null) ? "unknown" : result.getDevice()
						.getName();
				scanResultDevice.rssi = result.getRssi();

				mScanResultAdapter.addDevice(scanResultDevice);
				mScanResultAdapter.notifyDataSetChanged();
			}
		}

		@Override
		public void onScanFailed(int errorCode) {
			super.onScanFailed(errorCode);
			CSLog.i(SendDBActivity.class, "Scan failed with error: " + errorCode);

		}
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

	private boolean connect(String address) {

		CSLog.i(SendDBActivity.class, "connect address = " + address);

		if (mBluetoothAdapter == null || address == null) {
			CSLog.i(SendDBActivity.class, "BluetoothAdapter not initialized or unspecified address.");

			return false;
		}

		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			CSLog.i(SendDBActivity.class, "Device not found. Unable to connect.");

			return false;
		}

		if (mBluetoothGatt == null) {

			CSLog.i(SendDBActivity.class, "Trying to create a new connection.");
			mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
			refreshDeviceCache(mBluetoothGatt);

		}
		return true;
	}

	private void disconnect() {

		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			CSLog.i(SendDBActivity.class, "BluetoothAdapter not initialized");
			return;
		}

		CSLog.i(SendDBActivity.class, "start disconnect");
		mBluetoothGatt.disconnect();
	}

	private synchronized void gattclose() {
		if (mBluetoothGatt == null) {
			return;
		}
		refreshDeviceCache(mBluetoothGatt);
		mBluetoothGatt.close();
		mBluetoothGatt = null;
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
			CSLog.i(SendDBActivity.class, "An exception occured while refreshing device");
		}
		return false;
	}

	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

			if (newState == BluetoothProfile.STATE_CONNECTED) {

				mConnectStatus = STATE_CONNECTED;

				CSLog.i(SendDBActivity.class, "BluetoothProfile.STATE_CONNECTED");

				runOnUiThread(new Runnable() {
					public void run() {
						mProgressDialog.setMessage("Connected");
					}
				});

				mBluetoothGatt.discoverServices();

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				// here gattclose the gatt
				gattclose();
				CSLog.i(SendDBActivity.class, "BluetoothProfile.STATE_DISCONNECTED");

				if (mConnectStatus == STATE_CONNECTING) {

					gattclose();

					// reconnect
					CSLog.i(SendDBActivity.class, "-- reconnect --");
					connect(mSelectedAddress);
				} else {

					//
					runOnUiThread(new Runnable() {
						public void run() {
							// mProgressDialog.setMessage("Disconnected");

							CSLog.i(SendDBActivity.class, "currentsize " + currentsize + " totalsize " + totalsize);
							if (currentsize != totalsize) {
								mProgressDialog.dismiss();
								createDisconnectDialog();
							} else {
								mProgressDialog.cancel();
							}
						}
					});

					mConnectStatus = STATE_DISCONNECTED;

				}

			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {

			CSLog.i(SendDBActivity.class, "onServicesDiscovered GATT_SUCCESS");

			mQPPService = mBluetoothGatt.getService(UUID.fromString(MeshConstants.QPPS_SERVICE_UUID));

			if (mQPPService != null) {
				mQPPCharacteristic = mQPPService.getCharacteristic(UUID.fromString(MeshConstants.QPPS_CHAR_WRITE_UUID));

				mNotifyCharacteristic = mQPPService.getCharacteristic(UUID
						.fromString(MeshConstants.QPPS_CHAR_NOTIFY_UUID));

				setCharacteristicNotification(mNotifyCharacteristic, true);

			} else {

				disconnect();
			}

		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

			CSLog.i(SendDBActivity.class, "onCharacteristicRead GATT_SUCCESS");
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

			// CSLog.i(SendDBActivity.class,
			// "onCharacteristicWrite GATT_SUCCESS");

			mSemaphore.release();

			// CSLog.i(SendDBActivity.class, "mSemaphore.release");

		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

			CSLog.i(SendDBActivity.class, "onCharacteristicChanged");

			mAsyncTask.cancel(true);
			disconnect();
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

			CSLog.i(SendDBActivity.class, "onDescriptorWrite GATT_SUCCESS");

			if (Looper.myLooper() == Looper.getMainLooper()) {
				CSLog.i(SendDBActivity.class, "getMainLooper");
			} else {
				CSLog.i(SendDBActivity.class, "no");
			}

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					mAsyncTask = new ProgressAsyncTask();
					// mAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					mAsyncTask.execute();

				}
			});

		}
	};

	private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			CSLog.i(SendDBActivity.class, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(MeshConstants.CCCD_UUID));
		if (descriptor != null) {
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
	}

	private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			CSLog.i(SendDBActivity.class, "BluetoothAdapter not initialized");
			return false;
		}

		CSLog.i(SendDBActivity.class, "write value = " + CryptoUtils.toHex(data));

		characteristic.setValue(data);
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}

	FileInputStream mFileInputStream;
	Semaphore mSemaphore = new Semaphore(1);

	public class ProgressAsyncTask extends AsyncTask<Integer, Integer, String> {

		public ProgressAsyncTask() {
			super();
		}

		@Override
		protected String doInBackground(Integer... params) {

			// CSLog.i(ProgressAsyncTask.class, "doInBackground");

			try {
				while (mFileInputStream.available() > 0) {

					// CSLog.i(ProgressAsyncTask.class, "acquire");
					mSemaphore.acquire();

					if (mSendStatus == SEND_HEADER) {

						byte[] b = new byte[8];
						b[0] = (byte) 0xA5;
						b[1] = (byte) 0xA5;
						b[2] = (byte) 0xA5;
						b[3] = (byte) 0xA5;
						int filesize = mFileInputStream.available();
						b[4] = (byte) (filesize & 0xff);
						b[5] = (byte) ((filesize >> 8) & 0xff);
						b[6] = (byte) ((filesize >> 16) & 0xff);
						b[7] = (byte) ((filesize >> 24) & 0xff);

						totalsize = filesize;

						writeCharacteristic(mQPPCharacteristic, b);

						mSendStatus = SEND_BODY;

					} else if (mSendStatus == SEND_BODY) {

						publishProgress(mFileInputStream.available());

						byte[] b = new byte[20];
						int length = mFileInputStream.read(b, 0, 20);

						if (length < 20) {
							byte[] shortb = new byte[length];
							System.arraycopy(b, 0, shortb, 0, length);
							// CSLog.i(ProgressAsyncTask.class,
							// CryptoUtils.toHex(shortb));

							writeCharacteristic(mQPPCharacteristic, shortb);
						} else {

							// CSLog.i(ProgressAsyncTask.class,
							// CryptoUtils.toHex(b));
							writeCharacteristic(mQPPCharacteristic, b);
						}
					}

					// CSLog.i(ProgressAsyncTask.class, "writeCharacteristic");

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return "finish";
		}

		@Override
		protected void onPostExecute(String result) {
			// CSLog.i(ProgressAsyncTask.class, "onPostExecute");

			try {
				mSemaphore.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				mFileInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			currentsize = totalsize;

			byte[] b = { 0x5A, 0x5A, 0x5A, 0x5A };

			writeCharacteristic(mQPPCharacteristic, b);

		}

		@Override
		protected void onPreExecute() {
			// CSLog.i(ProgressAsyncTask.class, "onPreExecute");

			try {
				// File file = new
				// File(Environment.getExternalStorageDirectory()
				// + "/send/", "send.txt");

				File file = FileUtils.getStorageDirIntelligently(getApplicationContext(), MySyncConstants.SYNC_DIR + "/"
						+ MySyncConstants.SEND_FILE_NAME);

				mFileInputStream = new FileInputStream(file);

				mSendStatus = SEND_HEADER;

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			currentsize = totalsize - values[0];

			float progress = (float) (currentsize * 1.0 / totalsize);
			// CSLog.i(ProgressAsyncTask.class, "progress = " + progress +
			// " currentsize = " + currentsize + " totalsize = " + totalsize);
			mProgressDialog.setProgress((int) (progress * 100));

			mProgressDialog.setProgressNumberFormat(String.format("%1d byte / %2d byte", currentsize, totalsize));

		}

	}

	private void createDisconnectDialog() {

		final Builder builder = new AlertDialog.Builder(SendDBActivity.this);
		builder.setTitle("Disconnection");
		builder.setCancelable(false);
		builder.setPositiveButton(R.string.OK, new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {

				mAsyncTask.cancel(true);

				finish();
			}

		});

		builder.create().show();

	}

}
