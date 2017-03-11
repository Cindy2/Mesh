package com.nxp.ble.meshlib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.nxp.ble.meshlib.callback.IJoinCallback;
import com.nxp.ble.meshlib.callback.ILeaveCallback;
import com.nxp.ble.meshlib.callback.IProvisionCallback;
import com.nxp.ble.meshlib.callback.IStartScanCallback;
import com.nxp.ble.meshlib.callback.IUpdateStatusCallback;
import com.nxp.ble.meshlib.command.NoCallbackCommand;
import com.nxp.ble.meshlib.command.base.AbstractCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceAffiliationPO;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.DeviceOperationPO;
import com.nxp.utils.po.GroupInfoPO;
import com.nxp.utils.po.GroupOperationPO;
import com.nxp.utils.po.SwitchAffiliationPO;


public class MeshService extends Service {
    private BluetoothAdapter mBluetoothAdapter;
    private final IBinder mBinder = new MeshService.LocalBinder();
    private long mScanPeriod = 10000L;
    private IStartScanCallback mScanResultCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler mScanStopHandler;
    private Runnable mScanStopRunnable = new Runnable() {
        public void run() {
            CSLog.i(MeshService.class, "scan timeout and stop scan");
            if (MeshService.this.mScanResultCallback != null) {
                MeshService.this.mScanResultCallback.onTimeout();
            }

            MeshService.this.mScanResultCallback = null;
            MeshService.this.stopScanning();
        }
    };
    private Handler mJoinScanStopHandler;
    private Runnable mJoinScanStopRunnable = new Runnable() {
        public void run() {
            CSLog.i(MeshService.class, "JOIN scan timeout and stop scan");
            if (MeshService.this.mScanCallback != null) {
                MeshService.this.mBluetoothLeScanner.stopScan(MeshService.this.mScanCallback);
                MeshService.this.mScanCallback = null;
            }

            if (MeshService.this.mFindNearbyCallback != null) {
                MeshService.this.mFindNearbyCallback.onTimeout();
            }

            MeshService.this.mFindNearbyCallback = null;
        }
    };
    private static BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mQPPService;
    private BluetoothGattCharacteristic mQPPCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private int mConnectStatus = 3;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_DISCONNECTED = 3;
    private int mProvisionStep = 0;
    private static final int STEP_IDLE = 0;
    private static final int STEP_SETTING_NODEID = 1;
    private static final int STEP_SETTING_KEY = 2;
    private static final int STEP_SETTING_UUID = 3;
    private static final int STEP_SETTING_GROUP = 4;
    private static final int STEP_SETTING_SWITCH_TARGET_DEVICE = 4;
    private static final int STEP_SETTING_SWITCH_TARGET_GROUP = 5;
    private static final int STEP_SETTING_FINISH = 6;
    private static final int STEP_SETTING_COMPLETE = 7;
    private HashSet<Integer> mActiveDeviceSet;
    private boolean mPushFinished = false;
    private String mProvision_macAddress;
    private String mProvision_deviceName;
    private String mProvision_key;
    private List<Integer> mProvision_groupsId;
    private int mProvision_deviceType;
    private int mProvision_switchDeviceId;
    private int mProvision_switchGroupId;
    private IProvisionCallback mIProvisionCallback;
    private Handler mProvisionHandler;
    private long mProvisionPeriod;
    private Runnable mProvisionRunnable = new Runnable() {
        public void run() {
            CSLog.i(MeshService.class, "provision timeout; disconnect; gatt close");
            if (MeshService.this.mIProvisionCallback != null) {
                MeshService.this.mIProvisionCallback.onTimeout();
            }

            MeshService.this.disconnect();
            MeshService.this.gattclose();
        }
    };
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == 2) {
                CSLog.i(MeshService.class, "BluetoothProfile.STATE_CONNECTED");
                MeshService.this.mConnectStatus = 2;
                MeshService.mBluetoothGatt.discoverServices();
            } else if (newState == 0) {
                MeshService.this.gattclose();
                CSLog.i(MeshService.class, "BluetoothProfile.STATE_DISCONNECTED");
                if (MeshService.this.mConnectStatus == 1) {
                    if (MeshService.this.mIProvisionCallback != null) {
                        MeshService.this.mIProvisionCallback.onFailed(3);
                        Log.e("MeshService", "MeshService.this.mConnectStatus == 1");
                    }

                    MeshService.this.mProvisionHandler.removeCallbacks(MeshService.this.mProvisionRunnable);
                } else if (MeshService.this.mConnectStatus == 2) {
                    if (MeshService.this.mProvisionStep == 7) {
                        MeshService.this.mProvisionStep = 0;
                        if (MeshService.this.mIProvisionCallback != null) {
                            MeshService.this.mIProvisionCallback.onSuccess();
                        }

                        MeshService.this.mProvisionHandler.removeCallbacks(MeshService.this.mProvisionRunnable);
                    } else {
                        if (MeshService.this.mIProvisionCallback != null) {
                            MeshService.this.mIProvisionCallback.onFailed(4);
                        }

                        MeshService.this.mProvisionHandler.removeCallbacks(MeshService.this.mProvisionRunnable);
                        MeshService.this.deleteProvisionDatabase();
                    }
                }

                MeshService.this.mConnectStatus = 3;
            }

        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            CSLog.i(MeshService.class, "onServicesDiscovered GATT_SUCCESS");
            MeshService.this.mQPPService = MeshService.mBluetoothGatt.getService(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"));
            if (MeshService.this.mQPPService != null) {
                MeshService.this.mQPPCharacteristic = MeshService.this.mQPPService.getCharacteristic(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"));
                MeshService.this.mNotifyCharacteristic = MeshService.this.mQPPService.getCharacteristic(UUID.fromString("d44bc439-abfd-45a2-b575-925416129601"));
                MeshService.this.setCharacteristicNotification(MeshService.this.mNotifyCharacteristic, true);
            } else {
                if (MeshService.this.mIProvisionCallback != null) {
                    MeshService.this.mIProvisionCallback.onFailed(5);
                }

                MeshService.this.mProvisionHandler.removeCallbacks(MeshService.this.mProvisionRunnable);
                MeshService.this.disconnect();
            }

        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            CSLog.i(MeshService.class, "onCharacteristicRead GATT_SUCCESS");
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            CSLog.i(MeshService.class, "onCharacteristicWrite GATT_SUCCESS");
            if (MeshService.this.mProvisionStep == 1) {
                MeshService.this.writeProvisionKey();
            } else if (MeshService.this.mProvisionStep == 2) {
                MeshService.this.writeProvisonUUID();
            } else if (MeshService.this.mProvisionStep == 3) {
                if (MeshService.this.mProvision_deviceType == 10) {
                    if (MeshService.this.mProvision_switchDeviceId > 0) {
                        MeshService.this.writeSwitchTargetDevice();
                    } else if (MeshService.this.mProvision_switchGroupId > 0) {
                        MeshService.this.writeSwitchTargetGroup();
                    }
                } else {
                    MeshService.this.writeProvisionGroups();
                }
            } else if (MeshService.this.mProvisionStep == 4 || MeshService.this.mProvisionStep == 4 || MeshService.this.mProvisionStep == 5) {
                MeshService.this.writeProvisionFinish();
            }

        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            CSLog.i(MeshService.class, "onCharacteristicChanged");
            if (MeshService.this.mProvisionStep == 6) {
                MeshService.this.mProvisionStep = 7;
                MeshService.this.saveProvisionDatabase();
            }

            MeshService.this.disconnect();
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            CSLog.i(MeshService.class, "onDescriptorWrite GATT_SUCCESS");
            if (status == 0) {
                MeshService.this.prepareProvisionDatabase();
                MeshService.this.writeProvisionNodeId();
            } else {
                if (MeshService.this.mIProvisionCallback != null) {
                    MeshService.this.mIProvisionCallback.onFailed(5);
                }

                MeshService.this.mProvisionHandler.removeCallbacks(MeshService.this.mProvisionRunnable);
                MeshService.this.disconnect();
            }

        }
    };
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;
    private BluetoothGattServer mGattServer;
    private BluetoothGattCharacteristic mServerNotifyCharacteristic;
    private BluetoothDevice mServerNotifyDevice;
    private String mJoinAddress;
    private IStartScanCallback mFindNearbyCallback;
    private IJoinCallback mJoinCallback;
    private List<IUpdateStatusCallback> mDisconnectCallbackList;
    private String mNetworkVersion = "00";
    private int mJoinScanPeriold = 1000;
    private int mScanRssi = -100;
    private static final int RSSI_MIN = -100;
    private int mAdvDest = -1;
    private String mAdvDestAddr = "";
    private long mJoinPeriod = 30000L;
    private boolean mStartLeave = false;
    Handler mServerCloseHandler;
    Runnable mServerCloseRunnable = new Runnable() {
        public void run() {
            MeshService.this.gattserverclose();
        }
    };
    private Handler mJoinHandler;
    private Runnable mJoinRunnable = new Runnable() {
        public void run() {
            CSLog.i(MeshService.class, "join timeout and stop adv");
            MeshService.this.stopAdvertising();
            if (MeshService.this.mJoinCallback != null) {
                MeshService.this.mJoinCallback.onTimeout();
            }

        }
    };
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            CSLog.i(MeshService.class, "server connection new state = " + Integer.toString(newState));
            CSLog.i(MeshService.class, device.getAddress());
            if (newState == 2) {
                CSLog.i(MeshService.class, "BluetoothProfile.STATE_CONNECTED");
                MeshService.this.mStartLeave = false;
            } else if (newState == 0) {
                CSLog.i(MeshService.class, "BluetoothProfile.STATE_DISCONNECTED");
                MeshService.this.stopAdvertising();
                MeshService.this.mServerNotifyDevice = null;
                CSLog.i(MeshService.class, "disconnect mStartLeave " + MeshService.this.mStartLeave);
                if (!MeshService.this.mStartLeave) {
                    if (MeshService.this.mJoinCallback != null) {
                        MeshService.this.mJoinCallback.onFailed(2);
                    }

                    if (MeshService.this.mDisconnectCallbackList != null) {
                        Iterator var5 = MeshService.this.mDisconnectCallbackList.iterator();

                        while (var5.hasNext()) {
                            IUpdateStatusCallback callback = (IUpdateStatusCallback) var5.next();
                            callback.onDisconnect(1);
                        }
                    }
                } else {
                    MeshService.this.mStartLeave = false;
                }

                CSLog.i(MeshService.class, "after mStartLeave " + MeshService.this.mStartLeave);
                MeshService.this.mJoinHandler.removeCallbacks(MeshService.this.mJoinRunnable);
            }

        }

        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            CSLog.i(MeshService.class, "onServiceAdded status " + status);
        }

        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            CSLog.i(MeshService.class, "onCharacteristicReadRequest");
            CSLog.i(MeshService.class, "characteristic value = " + CryptoUtils.toHex(characteristic.getValue()));
            MeshService.this.mGattServer.sendResponse(device, requestId, 0, offset, characteristic.getValue());
        }

        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            CSLog.i(MeshService.class, "onCharacteristicWriteRequest");
            CSLog.i(MeshService.class, "value = " + CryptoUtils.toHex(value));
            characteristic.setValue(value);
            if (responseNeeded) {
                MeshService.this.mGattServer.sendResponse(device, requestId, 0, offset, characteristic.getValue());
            }

            try {
                byte[] var19;
                if (value[0] == 7 && value.length == 20) {
                    if (MeshService.this.mAdvDest > 0) {
                        DeviceInfoPO var14 = DeviceInfoPO.getDeviceInfoPO(MeshService.this.mAdvDest);
                        if (var14 == null) {
                            var14 = DeviceInfoPO.createDeviceInfoPO();
                            var14.setNodeId(MeshService.this.mAdvDest);
                            var14.setDeviceNo(device.getAddress());
                            var14.setDeviceName("newDevice");
                            if ((value[3] & 255) > 0 && (value[3] & 255) < 13) {
                                var14.setDeviceType(value[3] & 255);
                            } else {
                                var14.setDeviceType(4);
                            }

                            var14.save();
                        }
                    }

                    byte[] var15 = new byte[16];
                    System.arraycopy(value, 4, var15, 0, 16);
                    String var16 = PrivateData.getProvisionKey(MeshService.this.getApplicationContext());
                    byte[] var18 = CryptoUtils.encryptByte(CryptoUtils.toByte(var16), var15);
                    CSLog.i(MeshService.class, "encryptString  == " + CryptoUtils.toHex(var18));
                    var19 = new byte[]{(byte) 7, (byte) 0, (byte) 2, (byte) 16, (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10, (byte) 11, (byte) 12, (byte) 13, (byte) 14, (byte) 15};
                    System.arraycopy(var18, 0, var19, 4, 16);
                    CSLog.i(MeshService.class, "cmd = " + CryptoUtils.toHex(var19));
                    if (MeshService.this.mServerNotifyDevice != null && MeshService.this.mServerNotifyCharacteristic != null) {
                        MeshService.this.mServerNotifyCharacteristic.setValue(var19);
                        MeshService.this.mGattServer.notifyCharacteristicChanged(MeshService.this.mServerNotifyDevice, MeshService.this.mServerNotifyCharacteristic, false);
                    }

                    if (MeshService.this.mJoinCallback != null) {
                        MeshService.this.mJoinCallback.onUpdate(0, MeshService.this.mAdvDest);
                    }

                    return;
                }

                String e;
                byte[] decryptdata;
                if (value.length == 16) {
                    e = PrivateData.getProvisionKey(MeshService.this.getApplicationContext());
                    decryptdata = CryptoUtils.decryptByte(CryptoUtils.toByte(e), value);
                    CSLog.i(MeshService.class, "unencryptdata: " + CryptoUtils.toHex(decryptdata));
                    if (decryptdata[0] == 9) {
                        MeshService.this.mNetworkVersion = String.valueOf(decryptdata[4] & 255);
                    } else if (decryptdata[0] == 11) {
                        CSLog.i(MeshService.class, "-----RECEIVE INFO------");
                        int[] var17 = new int[16];

                        for (int ackdata = 0; ackdata < 16; ++ackdata) {
                            var17[ackdata] = decryptdata[ackdata] & 255;
                        }

                        MeshService.this.updateLocalDatabase(var17);
                        if (decryptdata[2] == 1) {
                            var19 = new byte[]{(byte) 11, (byte) 0, (byte) 2, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
                            byte[] cmd = CryptoUtils.encryptByte(CryptoUtils.toByte(e), var19);
                            if (MeshService.this.mServerNotifyDevice != null && MeshService.this.mServerNotifyCharacteristic != null) {
                                MeshService.this.mServerNotifyCharacteristic.setValue(cmd);
                                MeshService.this.mGattServer.notifyCharacteristicChanged(MeshService.this.mServerNotifyDevice, MeshService.this.mServerNotifyCharacteristic, false);
                            }
                        }

                        return;
                    }
                }

                if (value.length == 16) {
                    e = PrivateData.getProvisionKey(MeshService.this.getApplicationContext());
                    decryptdata = CryptoUtils.decryptByte(CryptoUtils.toByte(e), value);
                    CSLog.i(MeshService.class, "command response: " + CryptoUtils.toHex(decryptdata));
                    int commandId = MeshService.this.getResponseCommandId(decryptdata);
                    if (commandId != -1) {
                        MeshService.this.processResponse(commandId, decryptdata);
                    }
                }
            } catch (Exception var13) {
                var13.printStackTrace();
            }

        }

        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            CSLog.i(MeshService.class, "onNotificationSent status = " + status);
        }

        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            CSLog.i(MeshService.class, "onDescriptorReadRequest");
            CSLog.i(MeshService.class, "value = " + CryptoUtils.toHex(descriptor.getValue()));
            MeshService.this.mGattServer.sendResponse(device, requestId, 0, offset, descriptor.getValue());
        }

        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            CSLog.i(MeshService.class, "Our gatt server descriptor was written.");
            CSLog.i(MeshService.class, "device: " + device.getAddress());
            CSLog.i(MeshService.class, "value = " + CryptoUtils.toHex(value));
            if (MeshService.this.mAdvDestAddr.equals(device.getAddress())) {
                if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    MeshService.this.mActiveDeviceSet = new HashSet();
                    MeshService.this.mPushFinished = false;
                    CSLog.i(MeshService.class, "store notify device");
                    MeshService.this.mServerNotifyDevice = device;
                    MeshService.this.mJoinAddress = device.getAddress();
                    if (MeshService.this.mJoinCallback != null) {
                        MeshService.this.mJoinCallback.onSuccess();
                    }

                    MeshService.this.mJoinHandler.removeCallbacks(MeshService.this.mJoinRunnable);
                }
            } else {
                CSLog.i(MeshService.class, "!!! other device connect !!!");
                MeshService.this.stopAdvertising();
                MeshService.this.mServerNotifyDevice = null;
                if (MeshService.this.mJoinCallback != null) {
                    MeshService.this.mJoinCallback.onFailed(3);
                }

                MeshService.this.mJoinHandler.removeCallbacks(MeshService.this.mJoinRunnable);
            }

            if (responseNeeded) {
                MeshService.this.mGattServer.sendResponse(device, requestId, 0, offset, descriptor.getValue());
            }

        }

        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            CSLog.i(MeshService.class, "Our gatt server on execute write.");
        }
    };
    private Map<Integer, AbstractCommand> mCallbackCommands = new ConcurrentHashMap();
    private static final int INVALID_COMMAND_ID = -1;
    private static final int COMMAND_TIMEOUT_MSG_ID = 0;
    private static final int COMMAND_TIMEOUT = 5000;
    private MeshService.MyHandler mHandler;

    public MeshService() {
        this.mHandler = new MeshService.MyHandler(this.mCallbackCommands);
    }

    public void onCreate() {
        this.mBluetoothAdapter = ((BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        this.mBluetoothLeScanner = this.mBluetoothAdapter.getBluetoothLeScanner();
        this.mBluetoothLeAdvertiser = this.mBluetoothAdapter.getBluetoothLeAdvertiser();
        this.mScanStopHandler = new Handler();
        this.mJoinScanStopHandler = new Handler();
        this.mProvisionHandler = new Handler();
        this.mJoinHandler = new Handler();
        this.mServerCloseHandler = new Handler();
    }

    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        this.gattclose();
        this.gattserverclose();
        return super.onUnbind(intent);
    }

    public void startScanning(long timeoutPeriod, IStartScanCallback callback) {
        this.mScanPeriod = timeoutPeriod;
        this.mScanResultCallback = callback;
        if (this.mScanCallback == null) {
            CSLog.i(MeshService.class, "Starting Scanning");
            this.mScanStopHandler.postDelayed(this.mScanStopRunnable, this.mScanPeriod);
            this.mScanCallback = new MeshService.MyScanCallback();
            this.mBluetoothLeScanner.startScan(this.buildScanFilters(), this.buildScanSettings(), this.mScanCallback);
        } else {
            CSLog.i(MeshService.class, "already in scanning");
        }

    }

    public void stopScanning() {
        CSLog.i(MeshService.class, "Stopping Scanning");
        if (this.mScanCallback != null) {
            this.mBluetoothLeScanner.stopScan(this.mScanCallback);
            this.mScanCallback = null;
        }

        this.mScanResultCallback = null;
        this.mScanStopHandler.removeCallbacks(this.mScanStopRunnable);
    }

    private List<ScanFilter> buildScanFilters() {
        ArrayList scanFilters = new ArrayList();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        scanFilters.add(builder.build());
        return scanFilters;
    }

    private ScanSettings buildScanSettings() {
        android.bluetooth.le.ScanSettings.Builder builder = new android.bluetooth.le.ScanSettings.Builder();
        builder.setScanMode(2);
        return builder.build();
    }

    public void setupConfig(BLEDevice deviceInfo, IProvisionCallback callback) throws Exception {
        CSLog.i(MeshService.class, "setupConfig");
        this.mProvisionPeriod = deviceInfo.getTimeoutPeriod();
        this.mProvision_macAddress = deviceInfo.getMacAddress();
        this.mProvision_deviceName = deviceInfo.getDeviceName();
        this.mProvision_key = PrivateData.createProvisionKey(deviceInfo.getSetupKey());
        this.mProvision_deviceType = deviceInfo.getDeviceType();
        this.mProvision_groupsId = deviceInfo.getGroupsId();
        this.mProvision_switchDeviceId = deviceInfo.getSwitchControlDevice();
        this.mProvision_switchGroupId = deviceInfo.getSwitchControlGroup();
        this.mIProvisionCallback = callback;
        boolean setkey = true;
        if (deviceInfo.getSetupKey().equals("") && !PrivateData.isProvisionKeyExist(this.getApplicationContext())) {
            setkey = false;
            if (this.mIProvisionCallback != null) {
                this.mIProvisionCallback.onFailed(1);
            }
        }

        if (deviceInfo.getSetupKey().equals("") && PrivateData.isProvisionKeyExist(this.getApplicationContext())) {
            this.mProvision_key = PrivateData.getProvisionKey(this.getApplicationContext());
        }

        if (!deviceInfo.getSetupKey().equals("") && !PrivateData.isProvisionKeyExist(this.getApplicationContext())) {
            PrivateData.saveProvisionKey(this.getApplicationContext(), this.mProvision_key);
        }

        if (!deviceInfo.getSetupKey().equals("") && PrivateData.isProvisionKeyExist(this.getApplicationContext())) {
            String encryptionkey = PrivateData.getProvisionKey(this.getApplicationContext());
            if (!encryptionkey.equals(this.mProvision_key)) {
                setkey = false;
                if (this.mIProvisionCallback != null) {
                    this.mIProvisionCallback.onFailed(1);
                }
            }
        }

        if (DeviceInfoPO.getDeviceCount() >= 254) {
            if (this.mIProvisionCallback != null) {
                this.mIProvisionCallback.onFailed(2);
            }

        } else {
            if (setkey) {
                CSLog.i(MeshService.class, "start connect");
                this.mConnectStatus = 1;
                this.mProvisionStep = 0;
                this.connect(this.mProvision_macAddress);
                this.mProvisionHandler.postDelayed(this.mProvisionRunnable, this.mProvisionPeriod);
            } else {
                CSLog.i(MeshService.class, "provision key error");
            }

        }
    }

    private boolean connect(String address) {
        if (this.mBluetoothAdapter != null && address != null) {
            BluetoothDevice device = this.mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                CSLog.i(MeshService.class, "Device not found. Unable to connect.");
                if (this.mIProvisionCallback != null) {
                    Log.e("IProvisionCallback", "device == null");
                    this.mIProvisionCallback.onFailed(3);
                }

                return false;
            } else {
                if (mBluetoothGatt == null) {
                    CSLog.i(MeshService.class, "Trying to create a new connection.");
                    mBluetoothGatt = device.connectGatt(this, false, this.mGattCallback);
                    this.refreshDeviceCache(mBluetoothGatt);
                }

                return true;
            }
        } else {
            CSLog.i(MeshService.class, "BluetoothAdapter not initialized or unspecified address.");
            if (this.mIProvisionCallback != null) {
                Log.e("IProvisionCallback", "BluetoothAdapter not initialized or unspecified address.");
                this.mIProvisionCallback.onFailed(3);
            }

            return false;
        }
    }

    private void disconnect() {
        if (this.mBluetoothAdapter != null && mBluetoothGatt != null) {
            CSLog.i(MeshService.class, "start disconnect");
            mBluetoothGatt.disconnect();
        } else {
            CSLog.i(MeshService.class, "BluetoothAdapter not initialized");
        }
    }

    private synchronized void gattclose() {
        if (mBluetoothGatt != null) {
            this.refreshDeviceCache(mBluetoothGatt);
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
                return bool;
            }
        } catch (Exception var5) {
            CSLog.i(MeshService.class, "An exception occured while refreshing device");
        }

        return false;
    }

    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (this.mBluetoothAdapter != null && mBluetoothGatt != null) {
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }

        } else {
            CSLog.i(MeshService.class, "BluetoothAdapter not initialized");
        }
    }

    private boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        if (this.mBluetoothAdapter != null && mBluetoothGatt != null) {
            characteristic.setValue(data);
            return mBluetoothGatt.writeCharacteristic(characteristic);
        } else {
            CSLog.i(MeshService.class, "BluetoothAdapter not initialized");
            return false;
        }
    }

    private boolean filterMeshAdvertisement(ScanResult result) {
        List uuids = result.getScanRecord().getServiceUuids();
        boolean hasuuid = false;
        if (uuids != null) {
            Iterator manufacture = uuids.iterator();

            while (manufacture.hasNext()) {
                ParcelUuid data = (ParcelUuid) manufacture.next();
                if (data.toString().equals("0000fee9-0000-1000-8000-00805f9b34fb")) {
                    hasuuid = true;
                    break;
                }
            }
        }

        if (!hasuuid) {
            return false;
        } else {
            byte[] var8 = new byte[10];
            SparseArray var9 = result.getScanRecord().getManufacturerSpecificData();
            if (var9 != null && var9.size() > 0 && ((byte[]) var9.valueAt(0)).length == 8) {
                var8[1] = (byte) (var9.keyAt(0) & 255);
                var8[0] = (byte) (var9.keyAt(0) >> 8 & 255);

                for (int verify = 0; verify < 8; ++verify) {
                    var8[2 + verify] = ((byte[]) var9.valueAt(0))[verify];
                }

                byte var10 = 0;

                for (int i = 0; i < var8.length - 1; ++i) {
                    var10 += var8[i];
                }

                if (var10 == var8[9]) {
                    return true;
                }
            }

            return false;
        }
    }

    private String getMeshAdvAddress(ScanResult result) {
        SparseArray manufacture = result.getScanRecord().getManufacturerSpecificData();
        if (manufacture != null && manufacture.size() > 0 && ((byte[]) manufacture.valueAt(0)).length == 8) {
            StringBuilder sb = new StringBuilder();
            String stmp = "";

            for (int i = 0; i < 5; ++i) {
                stmp = Integer.toHexString(((byte[]) manufacture.valueAt(0))[4 - i] & 255);
                sb.append(stmp.length() == 1 ? "0" + stmp : stmp);
                sb.append(":");
            }

            stmp = Integer.toHexString(manufacture.keyAt(0) >> 8 & 255);
            sb.append(stmp.length() == 1 ? "0" + stmp : stmp);
            return sb.toString().toUpperCase(Locale.US).trim();
        } else {
            return null;
        }
    }

    private int getMeshAdvDeviceType(ScanResult result) {
        SparseArray manufacture = result.getScanRecord().getManufacturerSpecificData();
        return manufacture != null && manufacture.size() > 0 && ((byte[]) manufacture.valueAt(0)).length == 8 ? ((byte[]) manufacture.valueAt(0))[6] & 255 : -1;
    }

    private int getMeshValid(ScanResult result) {
        try {
            String e = PrivateData.getProvisionKey(this.getApplicationContext());
            String private_uuid = PrivateData.createUUID(e).toLowerCase(Locale.US);
            String uuid = "0000" + private_uuid + "-0000-1000-8000-00805f9b34fb";

            byte[] servicedata = result.getScanRecord().getServiceData(ParcelUuid.fromString(uuid));
            if (servicedata != null) {
                CSLog.i(MeshService.class, "service data: " + CryptoUtils.toHex(servicedata));
                if (servicedata[0] == 1) {
                    int isConnected = (servicedata[3] & 255) >> 6;
                    return isConnected;
                }
            }
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return 2;
    }

    private int getMeshNodeId(ScanResult result) {
        try {
            String e = PrivateData.getProvisionKey(this.getApplicationContext());
            String private_uuid = PrivateData.createUUID(e).toLowerCase(Locale.US);
            String uuid = "0000" + private_uuid + "-0000-1000-8000-00805f9b34fb";
            byte[] servicedata = result.getScanRecord().getServiceData(ParcelUuid.fromString(uuid));
            if (servicedata != null) {
                CSLog.i(MeshService.class, "service data: " + CryptoUtils.toHex(servicedata));
                int nodeid = servicedata[1] & 255;
                return nodeid;
            }
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return 0;
    }

    private void prepareProvisionDatabase() {
        DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(this.mProvision_macAddress);
        if (deviceInfoPO == null) {
            deviceInfoPO = DeviceInfoPO.createDeviceInfoPO();
            deviceInfoPO.setDeviceNo(this.mProvision_macAddress);
            deviceInfoPO.setDeviceName(this.mProvision_deviceName);
            deviceInfoPO.setDeviceType(this.mProvision_deviceType);
            deviceInfoPO.save();
        }

    }

    private void deleteProvisionDatabase() {
        DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(this.mProvision_macAddress);
        deviceInfoPO.remove();
    }

    private void saveProvisionDatabase() {
        DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(this.mProvision_macAddress);
        deviceInfoPO.setDeviceName(this.mProvision_deviceName);
        deviceInfoPO.setDeviceType(this.mProvision_deviceType);
        deviceInfoPO.save();
        if (this.mProvision_deviceType == 10) {
            CSLog.i(MeshService.class, "save data DEVICE_TYPE_SWITCH");
            CSLog.i(MeshService.class, "mProvision_switchDeviceId " + this.mProvision_switchDeviceId);
            CSLog.i(MeshService.class, "mProvision_switchGroupId " + this.mProvision_switchGroupId);
            deviceInfoPO.removeSwitchAffiliation();
            SwitchAffiliationPO groupId;
            if (this.mProvision_switchGroupId > 0) {
                groupId = SwitchAffiliationPO.createAffiliationPO(deviceInfoPO.getNodeId(), -1, this.mProvision_switchGroupId);
                groupId.save();
            } else if (this.mProvision_switchDeviceId > 0) {
                groupId = SwitchAffiliationPO.createAffiliationPO(deviceInfoPO.getNodeId(), this.mProvision_switchDeviceId, -1);
                groupId.save();
            }
        } else {
            CSLog.i(MeshService.class, "device type is not switch");
            deviceInfoPO.removeAffiliations();
            Iterator var3 = this.mProvision_groupsId.iterator();

            while (var3.hasNext()) {
                int groupId1 = ((Integer) var3.next()).intValue();
                GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(groupId1);
                DeviceAffiliationPO deviceAffiliationPO = DeviceAffiliationPO.createAffiliationPO(groupInfoPO.getGroupId(), deviceInfoPO.getNodeId());
                deviceAffiliationPO.save();
            }
        }

    }

    private void writeProvisionNodeId() {
        this.mProvisionStep = 1;
        int nodeid = DeviceInfoPO.getDeviceInfoPO(this.mProvision_macAddress).getNodeId();
        byte[] writedata = new byte[]{(byte) 1, (byte) 0, (byte) 0, (byte) 1, (byte) nodeid, (byte) 0};
        this.writeCharacteristic(this.mQPPCharacteristic, writedata);
        CSLog.i(MeshService.class, "NODEID: " + CryptoUtils.toHex(writedata));
    }

    private void writeProvisonUUID() {
        this.mProvisionStep = 3;
        String UUID = PrivateData.createUUID(this.mProvision_key);
        byte[] uuid = CryptoUtils.toByte(UUID);
        byte[] writedata = new byte[]{(byte) 3, (byte) 0, (byte) 0, (byte) 2, (byte) -65, (byte) -32};
        writedata[4] = uuid[1];
        writedata[5] = uuid[0];
        this.writeCharacteristic(this.mQPPCharacteristic, writedata);
        CSLog.i(MeshService.class, "UUID: " + CryptoUtils.toHex(writedata));
    }

    private void writeProvisionKey() {
        this.mProvisionStep = 2;
        byte[] writedata = new byte[20];
        writedata[0] = 2;
        writedata[3] = 16;
        byte[] key = CryptoUtils.toByte(this.mProvision_key);
        System.arraycopy(key, 0, writedata, 4, key.length);
        this.writeCharacteristic(this.mQPPCharacteristic, writedata);
        CSLog.i(MeshService.class, "key: " + CryptoUtils.toHex(writedata));
    }

    private void writeProvisionGroups() {
        this.mProvisionStep = 4;
        byte[] writedata = new byte[]{(byte) 4, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        int length = 0;

        for (Iterator var4 = this.mProvision_groupsId.iterator(); var4.hasNext(); ++length) {
            int groupId = ((Integer) var4.next()).intValue();
            writedata[4 + length] = (byte) groupId;
        }

        writedata[3] = (byte) length;
        this.writeCharacteristic(this.mQPPCharacteristic, writedata);
        CSLog.i(MeshService.class, "GROUP: " + CryptoUtils.toHex(writedata));
    }

    private void writeSwitchTargetDevice() {
        this.mProvisionStep = 4;
        byte[] writedata = new byte[]{(byte) 5, (byte) 0, (byte) 0, (byte) 1, (byte) this.mProvision_switchDeviceId};
        this.writeCharacteristic(this.mQPPCharacteristic, writedata);
        CSLog.i(MeshService.class, "switch target device: " + CryptoUtils.toHex(writedata));
    }

    private void writeSwitchTargetGroup() {
        this.mProvisionStep = 5;
        int groupId = 0;
        GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(this.mProvision_switchGroupId);
        if (groupInfoPO != null) {
            groupId = groupInfoPO.getGroupId();
        }

        byte[] writedata = new byte[]{(byte) 6, (byte) 0, (byte) 0, (byte) 1, (byte) groupId};
        this.writeCharacteristic(this.mQPPCharacteristic, writedata);
        CSLog.i(MeshService.class, "switch target group: " + CryptoUtils.toHex(writedata));
    }

    private void writeProvisionFinish() {
        this.mProvisionStep = 6;
        byte[] writedata = new byte[]{(byte) -64, (byte) 0, (byte) 0, (byte) 0};
        this.writeCharacteristic(this.mQPPCharacteristic, writedata);
        CSLog.i(MeshService.class, "FINISH: " + CryptoUtils.toHex(writedata));
    }

    public String getNetworkVersion() {
        return this.mNetworkVersion;
    }

    public void join(long timeoutPeriod, IJoinCallback callback) {
        CSLog.i(MeshService.class, "start join");
        this.mFindNearbyCallback = new IStartScanCallback() {
            public void onTimeout() {
                CSLog.i(MeshService.class, "mFindNearbyCallback timeout");
                if (MeshService.this.mScanRssi > -100) {
                    CSLog.i(MeshService.class, "startAdvertising");
                    MeshService.this.startAdvertising();
                } else {
                    CSLog.i(MeshService.class, "cannot find nearby");
                    if (MeshService.this.mJoinCallback != null) {
                        MeshService.this.mJoinCallback.onFailed(1);
                    }

                    MeshService.this.mJoinHandler.removeCallbacks(MeshService.this.mJoinRunnable);
                }

            }

            public void onScanResult(BLEDevice result) {
                CSLog.i(MeshService.class, "enter mFindNearbyCallback");
                String addr = result.getMacAddress();
                int rssi = result.getRSSI();
                if (result.getNodeId() > 0 && rssi > MeshService.this.mScanRssi) {
                    MeshService.this.mScanRssi = rssi;
                    int nodeid = result.getNodeId();
                    MeshService.this.mAdvDest = nodeid;
                    MeshService.this.mAdvDestAddr = addr;
                    CSLog.i(MeshService.class, "mScanRssi: mJoinAdv: " + MeshService.this.mScanRssi + " " + nodeid);
                }

            }

            public void onScanFailed(int errorCode) {
            }
        };
        if (this.mActiveDeviceSet != null) {
            this.mActiveDeviceSet.clear();
            this.mActiveDeviceSet = null;
        }

        this.mPushFinished = false;
        this.mJoinPeriod = timeoutPeriod;
        this.mJoinCallback = callback;
        this.mJoinHandler.postDelayed(this.mJoinRunnable, this.mJoinPeriod);
        this.mStartLeave = false;
        this.mScanRssi = -100;
        this.mAdvDest = -1;
        this.mAdvDestAddr = "";
        if (this.mScanCallback == null) {
            CSLog.i(MeshService.class, "Starting Scanning");
            this.mJoinScanStopHandler.postDelayed(this.mJoinScanStopRunnable, (long) this.mJoinScanPeriold);
            this.mScanCallback = new MeshService.MyScanCallback();
            this.mBluetoothLeScanner.startScan(this.buildScanFilters(), this.buildScanSettings(), this.mScanCallback);
        } else {
            CSLog.i(MeshService.class, "already in scanning");
        }

    }

    public void leave(ILeaveCallback callback) {
        CSLog.i(MeshService.class, "start leave");
        if (this.mActiveDeviceSet != null) {
            this.mActiveDeviceSet.clear();
            this.mActiveDeviceSet = null;
        }

        this.mPushFinished = false;
        this.mFindNearbyCallback = null;
        this.mJoinScanStopHandler.removeCallbacks(this.mJoinScanStopRunnable);
        if (this.mScanCallback != null) {
            this.mBluetoothLeScanner.stopScan(this.mScanCallback);
            this.mScanCallback = null;
        }

        this.mJoinHandler.removeCallbacks(this.mJoinRunnable);
        this.mStartLeave = true;
        CSLog.i(MeshService.class, "leave mStartLeave " + this.mStartLeave);
        this.stopAdvertising();
        if (callback != null) {
            callback.onSuccess();
        }

    }

    public void addUpdateStatusCallback(IUpdateStatusCallback callback) {
        if (this.mDisconnectCallbackList == null) {
            this.mDisconnectCallbackList = new ArrayList();
        }

        this.mDisconnectCallbackList.add(callback);
    }

    public void removeUpdateStatusCallback(IUpdateStatusCallback callback) {
        this.mDisconnectCallbackList.remove(callback);
    }

    private synchronized void gattserverclose() {
        CSLog.i(MeshService.class, "gattserverclose close");
        if (this.mGattServer != null) {
            CSLog.i(MeshService.class, "mGattServer close");
            this.mGattServer.close();
            this.mGattServer = null;
        }

    }

    private void startAdvertising() {
        CSLog.i(MeshService.class, "startAdvertising start");
        if (this.mGattServer == null) {
            this.mGattServer = ((BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE)).openGattServer(this, this.mGattServerCallback);
            this.addQPPService();
        }

        if (this.mAdvertiseCallback == null) {
            AdvertiseSettings settings = this.buildAdvertiseSettings();
            AdvertiseData data = this.buildAdvertiseData();
            this.mAdvertiseCallback = new MeshService.MyAdvertiseCallback();
            if (this.mBluetoothLeAdvertiser != null) {
                CSLog.i(MeshService.class, "Service: Starting Advertising");
                this.mBluetoothLeAdvertiser.startAdvertising(settings, data, this.mAdvertiseCallback);
            }
        }

    }

    public void stopAdvertising() {
        CSLog.i(MeshService.class, "stopAdvertising start");
        if (this.mBluetoothLeAdvertiser != null && this.mAdvertiseCallback != null) {
            CSLog.i(MeshService.class, "Service: Stopping Advertising");
            this.mBluetoothLeAdvertiser.stopAdvertising(this.mAdvertiseCallback);
            this.mAdvertiseCallback = null;
        }

        this.mServerCloseHandler.postDelayed(this.mServerCloseRunnable, 5L);
    }

    private AdvertiseData buildAdvertiseData() {
        android.bluetooth.le.AdvertiseData.Builder dataBuilder = new android.bluetooth.le.AdvertiseData.Builder();
        byte[] data = new byte[]{(byte) 2, (byte) this.mAdvDest, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        String key = "";

        try {
            key = PrivateData.getProvisionKey(this.getApplicationContext());
        } catch (Exception var6) {
            var6.printStackTrace();
        }

        String uuid16 = PrivateData.createUUID(key);
        String uuid = "0000" + uuid16.toLowerCase(Locale.US) + "-0000-1000-8000-00805f9b34fb";
        dataBuilder.addServiceData(ParcelUuid.fromString(uuid), data);
        CSLog.i(MeshService.class, "Adv data: " + CryptoUtils.toHex(data));
        CSLog.i(MeshService.class, "UUID: " + uuid);
        return dataBuilder.build();
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        android.bluetooth.le.AdvertiseSettings.Builder settingsBuilder = new android.bluetooth.le.AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(2);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    private void addQPPService() {
        CSLog.i(MeshService.class, "addQPPService");
        if (this.mGattServer == null) {
            CSLog.i(MeshService.class, "addQPPService mGattServer is null; return!");
        } else {
            BluetoothGattService gattService = this.mGattServer.getService(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"));
            if (gattService == null) {
                BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"), 12, 16);
                BluetoothGattDescriptor writeDescriptor = new BluetoothGattDescriptor(UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"), 1);
                writeDescriptor.setValue("1.2".getBytes());
                writeCharacteristic.addDescriptor(writeDescriptor);
                BluetoothGattCharacteristic notifyCharacteristic = new BluetoothGattCharacteristic(UUID.fromString("d44bc439-abfd-45a2-b575-925416129601"), 16, 1);
                BluetoothGattDescriptor notifyDescriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), 17);
                notifyDescriptor.setValue("0".getBytes());
                notifyCharacteristic.addDescriptor(notifyDescriptor);
                BluetoothGattService QPPService = new BluetoothGattService(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"), 0);
                QPPService.addCharacteristic(writeCharacteristic);
                QPPService.addCharacteristic(notifyCharacteristic);
                this.mServerNotifyCharacteristic = notifyCharacteristic;
                this.mGattServer.addService(QPPService);
            }

        }
    }

    public int getAdvDestForTest() {
        return this.mAdvDest;
    }

    public void sendCommand(AbstractCommand cmd) {
        if (this.mServerNotifyDevice != null && this.mServerNotifyCharacteristic != null) {
            CSLog.i(MeshService.class, "send command");
            if (cmd.getCommandCallback() != null) {
                this.mCallbackCommands.put(Integer.valueOf(cmd.getId()), cmd);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0, cmd.getId(), 0), 5000L);
            }

            CSLog.i(MeshService.class, "abs cmd: " + CryptoUtils.toHex(cmd.getCommandValue()));
            this.mServerNotifyCharacteristic.setValue(cmd.getCommandValue());
            this.mGattServer.notifyCharacteristicChanged(this.mServerNotifyDevice, this.mServerNotifyCharacteristic, false);
        } else {
            cmd.doResponse((byte[]) null);
        }

    }

    public void sendCommandWithoutCallback(byte[] cmd) {
        if (cmd.length <= 16) {
            byte[] sendcmd = new byte[16];
            if (cmd.length <= 16) {
                System.arraycopy(cmd, 0, sendcmd, 0, cmd.length);

                try {
                    String e = PrivateData.getProvisionKey(this.getApplicationContext());
                    byte[] encryptcmd = CryptoUtils.encryptByte(CryptoUtils.toByte(e), cmd);
                    this.sendCommand(new NoCallbackCommand(encryptcmd));
                } catch (Exception var5) {
                    var5.printStackTrace();
                }
            }

        }
    }

    private void processResponse(int commandId, byte[] value) {
        this.mHandler.removeMessages(0);
        AbstractCommand command = (AbstractCommand) this.mCallbackCommands.remove(Integer.valueOf(commandId));
        if (command != null) {
            command.doResponse(value);
        }

    }

    private int getResponseCommandId(byte[] value) {
        if (value != null && value.length > 5) {
            if (value[0] == 6) {
                return value[4] & 255;
            }

            if (value[0] == 8) {
                return value[4] & 255;
            }

            if (value[0] == 1) {
                return value[4] & 255;
            }
        }

        return -1;
    }

    private void updateLocalDatabase(int[] decryptdata) {
        IUpdateStatusCallback callback;
        Iterator groupOperationPO;
        int var19;
        if (decryptdata[3] == 0 && decryptdata[4] > 0) {
            DeviceInfoPO var21;
            DeviceOperationPO var30;
            if (decryptdata[7] == 10) {
                var19 = decryptdata[5];
                var21 = DeviceInfoPO.getDeviceInfoPO(var19);
                if (var21 == null) {
                    CSLog.i(MeshService.class, "create switch node id : " + var19);
                    var21 = DeviceInfoPO.createDeviceInfoPO();
                    var21.setNodeId(var19);
                    var21.setDeviceName("newDevice");
                    var21.save();
                }

                var21.setDeviceType(decryptdata[7]);
                var21.save();
                SwitchAffiliationPO var25 = SwitchAffiliationPO.getSwitchAffiliationPO(var21.getNodeId());
                if (var25 == null) {
                    var25 = SwitchAffiliationPO.createAffiliationPO(var21.getNodeId(), -1, -1);
                    var25.save();
                }

                if (decryptdata[9] == 0) {
                    var25.setDeviceId(decryptdata[8]);
                    var25.setGroupId(-1);
                } else if (decryptdata[9] == 1) {
                    GroupInfoPO var34 = GroupInfoPO.getGroupInfoPO(decryptdata[8]);
                    if (var34 == null) {
                        var34 = GroupInfoPO.createGroupInfoPO();
                        var34.setGroupId(decryptdata[8]);
                        var34.setGroupName("newGroup");
                        var34.setGroupType(0);
                        var34.save();
                    }

                    var25.setGroupId(decryptdata[8]);
                    var25.setDeviceId(-1);
                }

                var25.save();
                var30 = DeviceOperationPO.getDeviceOperationPO(var21.getNodeId());
                if (var30 == null) {
                    var30 = DeviceOperationPO.createOperationPO(var21.getNodeId());
                    var30.save();
                }

                if (decryptdata[6] == 1) {
                    CSLog.i(MeshService.class, "mActiveDeviceSet.add " + var21.getNodeId());
                    this.mActiveDeviceSet.add(Integer.valueOf(var21.getNodeId()));
                } else if (decryptdata[6] == 0) {
                    this.mActiveDeviceSet.remove(Integer.valueOf(var21.getNodeId()));
                } else if (decryptdata[6] == 2) {
                    var21.remove();
                }
            } else {
                var19 = decryptdata[5];
                CSLog.i(MeshService.class, "save node id : " + var19);
                var21 = DeviceInfoPO.getDeviceInfoPO(var19);
                DeviceInfoPO var24 = DeviceInfoPO.getDeleteDeviceInfoPO(var19);
                if (var21 == null && var24 != null) {
                    CSLog.i(MeshService.class, "Node has been deleted by the phone, return !!!");
                    return;
                }

                if (var21 == null) {
                    var21 = DeviceInfoPO.createDeviceInfoPO();
                    var21.setNodeId(var19);
                    var21.setDeviceName("newDevice");
                    var21.save();
                }

                var21.setDeviceType(decryptdata[7]);
                var21.save();
                var21.removeAffiliations();

                for (int var28 = 0; var28 < 2; ++var28) {
                    if (decryptdata[var28 + 8] != 0) {
                        int var31 = decryptdata[var28 + 8];
                        CSLog.i(MeshService.class, "save group id : " + var31);
                        GroupInfoPO var35 = GroupInfoPO.getGroupInfoPO(var31);
                        if (var35 == null) {
                            var35 = GroupInfoPO.createGroupInfoPO();
                            var35.setGroupId(var31);
                            var35.setGroupName("newGroup");
                            var35.setGroupType(decryptdata[7]);
                            var35.save();
                        }

                        DeviceAffiliationPO var36 = DeviceAffiliationPO.createAffiliationPO(decryptdata[var28 + 8], var21.getNodeId());
                        var36.save();
                    }
                }

                var30 = DeviceOperationPO.getDeviceOperationPO(var21.getNodeId());
                if (var30 == null) {
                    var30 = DeviceOperationPO.createOperationPO(var21.getNodeId());
                    var30.save();
                }

                if (decryptdata[6] == 1) {
                    var30.setValue1(decryptdata[10]);
                    var30.setValue3(decryptdata[12]);
                    var30.setValue2(decryptdata[11]);
                    var30.save();
                    CSLog.i(MeshService.class, "mActiveDeviceSet.add " + var21.getNodeId());
                    this.mActiveDeviceSet.add(Integer.valueOf(var21.getNodeId()));
                } else if (decryptdata[6] == 0) {
                    this.mActiveDeviceSet.remove(Integer.valueOf(var21.getNodeId()));
                } else if (decryptdata[6] == 2) {
                    var21.remove();
                }

                if (var21 != null) {
                    boolean var32 = decryptdata[10] != 0;
                    int[] var37 = var21.getGroupAffiliations();
                    int[] var11 = var37;
                    int var10 = var37.length;

                    for (int var9 = 0; var9 < var10; ++var9) {
                        int var38 = var11[var9];
                        int groupid = var38;
                        GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(var38);
                        List deviceInfoPOs1 = groupInfoPO.getDeviceListAffiliationsExceptSwitch();
                        boolean sameChecked = true;
                        Iterator var17 = deviceInfoPOs1.iterator();

                        while (var17.hasNext()) {
                            DeviceInfoPO groupOperationPO1 = (DeviceInfoPO) var17.next();
                            DeviceOperationPO deviceOperation = DeviceOperationPO.getDeviceOperationPO(groupOperationPO1.getNodeId());
                            CSLog.i(MeshService.class, "Device Impact Group: groupid " + groupid + " deviceid " + groupOperationPO1.getNodeId() + " checked " + deviceOperation.getValue1());
                            if (var32 != (deviceOperation.getValue1() != 0)) {
                                sameChecked = false;
                            }
                        }

                        if (sameChecked) {
                            GroupOperationPO var39 = GroupOperationPO.getGroupOperationPO(groupid);
                            if (var39 != null) {
                                var39.setValue1(!var32 ? 0 : 1);
                                var39.save();
                            }
                        }
                    }
                }
            }

            CSLog.i(MeshService.class, "update view");
            if (this.mJoinCallback != null) {
                this.mJoinCallback.onUpdate(decryptdata[3], decryptdata[5]);
            }

            if (this.mDisconnectCallbackList != null) {
                groupOperationPO = this.mDisconnectCallbackList.iterator();

                while (groupOperationPO.hasNext()) {
                    callback = (IUpdateStatusCallback) groupOperationPO.next();
                    callback.onUpdate(decryptdata[3], decryptdata[5]);
                }
            }
        } else if (decryptdata[3] == 1 && decryptdata[4] > 0) {
            var19 = decryptdata[5];
            GroupOperationPO var20 = GroupOperationPO.getGroupOperationPO(var19);
            if (var20 == null) {
                var20 = GroupOperationPO.createOperationPO(var19);
                var20.save();
            }

            var20.setValue1(decryptdata[7]);
            if (decryptdata[9] != 0) {
                var20.setValue3(decryptdata[9]);
            }

            if (decryptdata[8] != 0) {
                var20.setValue2(decryptdata[8]);
            }

            var20.save();
            if (var19 == 0) {
                List var22 = DeviceInfoPO.getDeviceListExceptSwitch();
                if (var22 != null) {
                    Iterator var29 = var22.iterator();

                    while (var29.hasNext()) {
                        DeviceInfoPO var26 = (DeviceInfoPO) var29.next();
                        DeviceOperationPO var33 = DeviceOperationPO.getDeviceOperationPO(var26.getNodeId());
                        if (var33 != null) {
                            var33.setValue1(decryptdata[7]);
                            if (decryptdata[9] != 0) {
                                var33.setValue3(decryptdata[9]);
                            }

                            if (decryptdata[8] != 0) {
                                var33.setValue2(decryptdata[8]);
                            }

                            var33.save();
                        }
                    }
                }
            } else {
                GroupInfoPO callback1 = GroupInfoPO.getGroupInfoPO(var19);
                if (callback1 != null) {
                    List deviceInfoPOs = callback1.getDeviceListAffiliationsExceptSwitch();
                    if (deviceInfoPOs != null) {
                        Iterator deviceOperationPO = deviceInfoPOs.iterator();

                        while (deviceOperationPO.hasNext()) {
                            DeviceInfoPO deviceInfoPO = (DeviceInfoPO) deviceOperationPO.next();
                            DeviceOperationPO deviceOperationPO1 = DeviceOperationPO.getDeviceOperationPO(deviceInfoPO.getNodeId());
                            if (deviceOperationPO1 != null) {
                                deviceOperationPO1.setValue1(decryptdata[7]);
                                if (decryptdata[9] != 0) {
                                    deviceOperationPO1.setValue3(decryptdata[9]);
                                }

                                if (decryptdata[8] != 0) {
                                    deviceOperationPO1.setValue2(decryptdata[8]);
                                }

                                deviceOperationPO1.save();
                            }
                        }
                    }
                }
            }

            if (this.mDisconnectCallbackList != null) {
                Iterator var27 = this.mDisconnectCallbackList.iterator();

                while (var27.hasNext()) {
                    IUpdateStatusCallback var23 = (IUpdateStatusCallback) var27.next();
                    var23.onUpdate(decryptdata[3], decryptdata[5]);
                }
            }
        } else if (decryptdata[4] == 0) {
            CSLog.i(MeshService.class, "update finished");
            CSLog.i(MeshService.class, "Active Set: " + this.mActiveDeviceSet.toString());
            this.mPushFinished = true;
            if (this.mJoinCallback != null) {
                this.mJoinCallback.onUpdate(decryptdata[3], decryptdata[5]);
            }

            if (this.mDisconnectCallbackList != null) {
                groupOperationPO = this.mDisconnectCallbackList.iterator();

                while (groupOperationPO.hasNext()) {
                    callback = (IUpdateStatusCallback) groupOperationPO.next();
                    callback.onUpdate(decryptdata[3], decryptdata[5]);
                }
            }
        }

    }

    public boolean isActiveDevice(int nodeId) {
        return this.mPushFinished ? this.mActiveDeviceSet.contains(Integer.valueOf(nodeId)) : true;
    }

    public String getOTADeviceAddress() {
        return this.mJoinAddress;
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        public MeshService getService() {
            return MeshService.this;
        }
    }

    private class MyAdvertiseCallback extends AdvertiseCallback {
        private MyAdvertiseCallback() {
        }

        public void onStartFailure(int errorCode) {
            CSLog.i(MeshService.class, "Advertising failed error = " + errorCode);
        }

        public void onStartSuccess(AdvertiseSettings settings) {
            CSLog.i(MeshService.class, "Advertising successfully started");
        }
    }

    private static class MyHandler extends Handler {
        private WeakReference<Map<Integer, AbstractCommand>> mCallbackCommandsRef;

        public MyHandler(Map<Integer, AbstractCommand> callbackCommands) {
            this.mCallbackCommandsRef = new WeakReference(callbackCommands);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                int commandId = msg.arg1;
                Map callbackCommands = (Map) this.mCallbackCommandsRef.get();
                if (callbackCommands != null) {
                    AbstractCommand command = (AbstractCommand) callbackCommands.remove(Integer.valueOf(commandId));
                    if (command != null && command.getCommandCallback() != null) {
                        command.getCommandCallback().onTimeOut();
                    }
                }
            }

        }
    }

    private class MyScanCallback extends ScanCallback {
        private MyScanCallback() {
        }

        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BLEDevice deviceInfo;
            if (MeshService.this.filterMeshAdvertisement(result)) {
                deviceInfo = new BLEDevice();
                deviceInfo.setMacAddress(result.getDevice().getAddress());
                deviceInfo.setDeviceName(result.getDevice().getName() == null ? "unknown" : result.getDevice().getName());
                deviceInfo.setRSSI(result.getRssi());
                deviceInfo.setDeviceType(MeshService.this.getMeshAdvDeviceType(result));
                CSLog.i(MeshService.class, "mScanResultCallback scan result: " + deviceInfo.getMacAddress());
                if (MeshService.this.mScanResultCallback != null) {
                    MeshService.this.mScanResultCallback.onScanResult(deviceInfo);
                }
            }

            if (MeshService.this.mFindNearbyCallback != null && MeshService.this.getMeshValid(result) == 0) {
                deviceInfo = new BLEDevice();
                deviceInfo.setMacAddress(result.getDevice().getAddress());
                deviceInfo.setRSSI(result.getRssi());
                deviceInfo.setNodeId(MeshService.this.getMeshNodeId(result));
                CSLog.i(MeshService.class, "mFindNearbyCallback scan result: " + deviceInfo.getMacAddress());
                MeshService.this.mFindNearbyCallback.onScanResult(deviceInfo);
            }

        }

        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            CSLog.i(MeshService.class, "Scan failed with error: " + errorCode);
            if (MeshService.this.mScanResultCallback != null) {
                MeshService.this.mScanResultCallback.onScanFailed(errorCode);
            }

        }
    }
}
