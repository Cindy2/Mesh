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

package com.beesmart.blemesh.activities;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.ble.otalib.BluetoothLeInterface;
import com.nxp.ble.otalib.bleGlobalVariables;
import com.nxp.ble.otalib.bleGlobalVariables.otaResult;
import com.nxp.ble.otalib.otaManager;
import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.exception.NotAvailableStorageException;
import com.nxp.utils.io.FileUtils;
import com.nxp.utils.log.CSLog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.services.BluetoothLeService;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */

public class DeviceOTAActivity extends AppCompatActivity {
	private final static String TAG = DeviceOTAActivity.class.getSimpleName();
	private String mDefaultFirmwarePath = null;
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static final String SELETED_FILE_NAME = "SELETED_FILE_NAME";
	public static final int UPDATE_DATA = 1;
	public static final int ERROR_CODE = 2;
	private TextView mConnectionState;
	private String mDeviceName;
	private String mDeviceAddress;
	private Button mLoadOTAUpdate;
	private ListView mFileListView;
	private FileArrayAdapter mFileAdapter;
	private boolean mConnected = false;
	private boolean mStarted = false;
	private static ProgressDialog progressDialog;
	private BluetoothLeService mBluetoothLeService;
	private otaManager updateManager = new otaManager();
	private boolean mStopUpdate = false;
	
	private Handler mTimeoutHandler;
	private Runnable mTimeoutRunnable = new Runnable() {

		@Override
		public void run() {
			Log.i(TAG, "mTimeoutRunnable timeout");

			createQuitDialog();
		}
		
	};
	
	private void createQuitDialog() {
		final Builder builder = new AlertDialog.Builder(DeviceOTAActivity.this);
		builder.setTitle(R.string.exit_OTA);
		builder.setCancelable(false);
		builder.setPositiveButton(R.string.OK, new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				setResult(0);
				finish();
			}
		});

		builder.create().show();
	}


	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			Log.i(TAG, "onServiceConnected");
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				setResult(0);
				finish();
			}

			mTimeoutHandler.postDelayed(mTimeoutRunnable, 15000);

			//scan
			mConnected = true;
			mStarted = true;
			invalidateOptionsMenu();
			updateConnectionState("Scanning");
			mBluetoothLeService.startScanning();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			Log.d(TAG, "mGattUpdateReceiver action:" + action);

			if (BluetoothLeService.ACTION_SCAN_RESULT.equals(action)) {
				String address = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);

				if (address.equals(mDeviceAddress) && mConnectionState.getText().toString().equals("Scanning")) {
					updateConnectionState("find device");
					mBluetoothLeService.stopScanning();
					// Automatically connects to the device upon successful start-up
					// initialization.

					updateConnectionState("Connecting");
					mBluetoothLeService.connect(mDeviceAddress);
				}
			}

			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				updateConnectionState("Connected");
				mConnected = true;
				invalidateOptionsMenu();

			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

				if (mConnectionState.getText().toString().equals("Connecting")) {
					// reconnect
					Log.i(TAG, "-- reconnect --");
					mConnected = true;
					mStarted = true;
					invalidateOptionsMenu();
					updateConnectionState("Connecting");
					mBluetoothLeService.connect(mDeviceAddress);
				} else {
					mConnected = false;
					mStarted = false;
					updateManager.otaStop();
					mStopUpdate = true;
					updateConnectionState("Disconnected");
					invalidateOptionsMenu();
				}

			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				mConnected = true;
				mStarted = false;
				invalidateOptionsMenu();

				if (isOtaServiceSupported()) {
					updateConnectionState("OTA service is ready");
				} else {
					updateConnectionState("No OTA service");
				}

				Log.i(TAG, "mTimeoutHandler remove callback");
				mTimeoutHandler.removeCallbacks(mTimeoutRunnable);

			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				byte[] notifyData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				updateManager.otaGetResult(notifyData);
			} else if (BluetoothLeService.ACTION_WRITE_STATUS.equals(action)) {
				final int defaultVal = 0xff;
				int status = intent.getIntExtra(action, defaultVal);
				if (status == BluetoothGatt.GATT_SUCCESS)
					updateManager.notifyWriteDataCompleted();
				else {
					String errCode = "Gatt write fail,errCode:" + String.valueOf(status);
					SendUpdateMsg(ERROR_CODE, "ERROR_CODE", errCode);
					mStopUpdate = true;
				}

			}
		}
	};

	private static String otaError2String(otaResult ret) {
		switch (ret) {
		case OTA_RESULT_SUCCESS:
			return "SUCCESS";
		case OTA_RESULT_PKT_CHECKSUM_ERROR:
			return "Transmission is failed,firmware checksum error";
		case OTA_RESULT_PKT_LEN_ERROR:
			return "Transmission is failed,packet length error";
		case OTA_RESULT_DEVICE_NOT_SUPPORT_OTA:
			return "The OTA function is disabled by the server";
		case OTA_RESULT_FW_SIZE_ERROR:
			return "Transmission is failed,firmware file size error";
		case OTA_RESULT_FW_VERIFY_ERROR:
			return "Transmission is failed,verify failed";
		case OTA_RESULT_OPEN_FIRMWAREFILE_ERROR:
			return "Open firmware file failed";
		case OTA_RESULT_META_RESPONSE_TIMEOUT:
			return "Wait meta packet response timeout";
		case OTA_RESULT_DATA_RESPONSE_TIMEOUT:
			return "Wait data packet response timeout";
		case OTA_RESULT_SEND_META_ERROR:
			return "Send meta data error";
		case OTA_RESULT_RECEIVED_INVALID_PACKET:
			return "Transmission is failed,received invalid packet";
		case OTA_RESULT_INVALID_ARGUMENT:
		default:
			return "Unknown error";
		}
	}

	private boolean isOtaServiceSupported() {
		if (mBluetoothLeService.getGattService(bleGlobalVariables.UUID_QUINTIC_OTA_SERVICE) != null)
			return true;
		return false;
	}

	private static String generateDisplayMsg(String title, int elapsedTime, int byteRate) {
		return new String(title + "\n" + elapsedTime + " s" + "\n" + byteRate + " Bps");
	}

	private void startOtaUpdate(String filename) {
		updateInstance ins = new updateInstance();
		ins.bleInterfaceInit(mBluetoothLeService.getBluetoothGatt());
		if (updateManager.otaStart(filename, ins) == otaResult.OTA_RESULT_SUCCESS) {
			updateProgress("OTA Update", generateDisplayMsg("Updating...", 0, 0));
		} else {
			Log.e(TAG, "onListItemClick:Faild to otaStart");
		}
	}

	private int getFirmwareFileList(String Extension) {
		String Path = mDefaultFirmwarePath;
		File current = new File(Path);
		if (!current.exists()) {
			Log.e(TAG, Path + ":No such file or directory");
			return -1;
		}
		if (!current.canRead()) {
			Log.e(TAG, ":No permission to open " + Path);
			return -2;
		}
		File[] files = current.listFiles();
		Log.i(TAG, "List files under " + Path + ":");
		for (File f : files) {
			if (f.isFile()) {
				if (!current.canRead()) {
					Log.w(TAG, ":No permission to read file " + Path + ",skipped!");
					continue;
				}
				if (f.getPath().substring(f.getPath().length() - Extension.length()).equals(Extension)) {
					Log.i(TAG, "add file: " + f.getName() + " size: " + f.length());
					mFileAdapter.add(f.getName(), f.length());
					mFileAdapter.notifyDataSetChanged();
				}
			}
		}
		if (mFileAdapter.getCount() == 0) {
			Toast.makeText(getApplicationContext(), Path + " is empty", Toast.LENGTH_LONG).show();
			return -3;
		}
		return 0;
	}

	private class FileArrayAdapter extends BaseAdapter {
		private ArrayList<HashMap<String, Object>> fileList;
		private LayoutInflater mInflater;

		public FileArrayAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
			fileList = new ArrayList<HashMap<String, Object>>();
		}

		void add(String name, long size) {
			HashMap<String, Object> item = new HashMap<String, Object>();
			item.put("filename", name);
			item.put("filesize", String.valueOf(size) + " bytes");
			fileList.add(item);
		}

		public int getCount() {
			return fileList.size();
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		public HashMap<String, Object> getItem(int position) {
			return fileList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public void clear() {
			fileList.clear();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listview_file, parent, false);
				holder = new ViewHolder();
				holder.FileName = (TextView) convertView.findViewById(R.id.ItemFileName);
				holder.FileSize = (TextView) convertView.findViewById(R.id.ItemFileSize);
				// holder.FileName.setEllipsize(android.text.TextUtils.TruncateAt.END);
				// holder.FileName.setMarqueeRepeatLimit(1);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			HashMap<String, Object> item = fileList.get(position);

			String fileName = (String) item.get("filename"); // get file path
			String fileSize = (String) item.get("filesize"); // get file size

			holder.FileName.setText(fileName);
			holder.FileSize.setText(fileSize);

			return convertView;
		}

		public final class ViewHolder {
			public TextView FileName;
			public TextView FileSize;
		}
	}

	private void listBinFiles() {
		getFirmwareFileList("bin");
	}

	Button.OnClickListener mloadFirmwareListener = new Button.OnClickListener() {
		public void onClick(View v) {
			if (!mConnected || mConnected && mStarted) {
				Toast.makeText(getApplicationContext(), "Connect bluetooth fisrt", Toast.LENGTH_SHORT).show();
				return;
			}
			if (!isOtaServiceSupported()) {
				Toast.makeText(getApplicationContext(), "The device doesn't support OTA service", Toast.LENGTH_SHORT)
						.show();
				return;
			}
			mFileAdapter.clear();
			mFileAdapter.notifyDataSetInvalidated();
			listBinFiles();
		}
	};

	public void updateProgress(String title, String message) {
		mStopUpdate = false;

		Thread updateThread = new Thread(update);
		updateThread.start();

		progressDialog = new ProgressDialog(this);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMessage(message);
		progressDialog.setTitle(title);
		/*
		 * //progressDialog.
		 * progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
		 * new DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int whichButton) { mStopUpdate=true;
		 * mBluetoothLeService.disconnect(); } });
		 */
		// progressDialog.
		progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				mStopUpdate = true;
				setResult(1);
				finish();
			}
		});
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setCancelable(false);
		progressDialog.setProgress(0);
		progressDialog.setMax(100);

		progressDialog.show();
	}

	private static Handler mHandler = new Handler() {
		int percent = 0;
		int byteRate = 0;
		int elapsedTime = 0;

		public void handleMessage(Message msg) {

			if (!Thread.currentThread().isInterrupted()) {
				switch (msg.what) {
				case UPDATE_DATA:
					int[] data = msg.getData().getIntArray("UPDATE_DATA");
					percent = data[0];
					byteRate = data[1];
					elapsedTime = data[2];
					// Log.d(TAG,"per:"+percent+" bps:"+byteRate+" time:"+elapsedTime);
					if (percent < progressDialog.getMax()) {
						progressDialog.setProgress(percent);
						progressDialog.setMessage(generateDisplayMsg("Updating...", elapsedTime, byteRate));
					} else {
						progressDialog.setProgress(percent);
						progressDialog.setMessage(generateDisplayMsg("Update Success", elapsedTime, byteRate));
					}
					break;
				case ERROR_CODE:
					String errStr = "Update Fail: " + msg.getData().getString("ERROR_CODE");
					progressDialog.setProgress(percent);
					progressDialog.setMessage(generateDisplayMsg(errStr, elapsedTime, byteRate));
					break;
				}
			}
		}
	};

	private void SendUpdateMsg(int type, String key, int[] value) {
		Message msg = new Message();
		msg.what = type;
		msg.getData().putIntArray(key, value);
		if (mHandler != null)
			mHandler.sendMessage(msg);
	}

	private void SendUpdateMsg(int type, String key, String str) {
		Message msg = new Message();
		msg.what = type;
		msg.getData().putString(key, str);
		if (mHandler != null)
			mHandler.sendMessage(msg);
	}

	Runnable update = new Runnable() {
		public void run() {
			int[] extra = new int[8];
			while (!mStopUpdate) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (!Thread.currentThread().isInterrupted()) {
					otaResult ret = updateManager.otaGetProcess(extra);
					if (ret == otaResult.OTA_RESULT_SUCCESS)
						SendUpdateMsg(UPDATE_DATA, "UPDATE_DATA", extra);
					else {
						updateManager.otaStop();
						mStopUpdate = true;
						SendUpdateMsg(ERROR_CODE, "ERROR_CODE", otaError2String(ret));
					}
				}
			}
		}
	};

	private class updateInstance extends BluetoothLeInterface {
		@Override
		public boolean bleInterfaceInit(BluetoothGatt bluetoothGatt) {
			return super.bleInterfaceInit(bluetoothGatt);
		}
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		// final Intent intent = new Intent(this, DeviceScanActivity.class);
		// startActivity(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ota);
		// mDefaultFirmwarePath=this.getExternalFilesDir("").getAbsolutePath();
		try {
			File dir = FileUtils.getStorageDirIntelligently(this, MeshConstants.OTA_FILE_BASE);
			mDefaultFirmwarePath = dir.getAbsolutePath();
		} catch (NotAvailableStorageException e) {
			CSLog.e(getClass(), "Dir specified is not available");
		}

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		mFileListView = (ListView) findViewById(R.id.filelist);
		mFileAdapter = new FileArrayAdapter(this);
		mFileListView.setAdapter(mFileAdapter);
		mFileListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FileArrayAdapter adapter = (FileArrayAdapter) parent.getAdapter();
				HashMap<String, Object> item = adapter.getItem(position);
				String filename = (String) item.get("filename"); // get file
																	// path
				String filePath = mDefaultFirmwarePath + "/" + filename;
				startOtaUpdate(filePath);
			}
		});

		// Sets up UI references.
		((TextView) findViewById(R.id.device_name)).setText(mDeviceName);
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mLoadOTAUpdate = (Button) findViewById(R.id.load_otaupdate_btn);
		mLoadOTAUpdate.setOnClickListener(mloadFirmwareListener);

		getSupportActionBar().setTitle("Devices OTA");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Log.i(TAG, "start bind");
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		
		mTimeoutHandler = new Handler();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		// if (mBluetoothLeService != null) {
		// if (!mBluetoothLeService.getConnectionState()) {
		// mConnected = false;
		// mStarted = false;
		// updateConnectionState("Disconnected");
		// invalidateOptionsMenu();
		// mBluetoothLeService.connect(mDeviceAddress);
		// }
		// }
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mConnectionState.getText().toString().equals("Scanning")) {
			mBluetoothLeService.stopScanning();
		} else {
			mBluetoothLeService.disconnect();
		}
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_ota, menu);
		menu.findItem(R.id.ota_connect).setVisible(false);
		menu.findItem(R.id.ota_disconnect).setVisible(false);
		
//		if (mConnected) {
//			menu.findItem(R.id.ota_connect).setVisible(false);
//			menu.findItem(R.id.ota_disconnect).setVisible(true);
//		} else {
//			menu.findItem(R.id.ota_connect).setVisible(true);
//			menu.findItem(R.id.ota_disconnect).setVisible(false);
//		}

		if (mStarted) {
			menu.findItem(R.id.ota_refresh).setActionView(R.layout.actionbar_progress);
		} else {
			menu.findItem(R.id.ota_refresh).setActionView(null);
		}
		return true;
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return super.onSupportNavigateUp();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ota_connect:
			if (!mConnected) {
				mConnected = true;
				mStarted = true;
				invalidateOptionsMenu();
				updateConnectionState("Connecting");
				mBluetoothLeService.connect(mDeviceAddress);
			}
			return true;
		case R.id.ota_disconnect:
			if (mConnected) {
				mConnected = false;
				mStarted = false;
				invalidateOptionsMenu();
				updateConnectionState("Disconnected");
				mBluetoothLeService.disconnect();
			}
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final String status) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectionState.setText(status);
			}
		});
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_SCAN_RESULT);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_WRITE_STATUS);
		return intentFilter;
	}
}
