package com.nxp.ble.otalib;

import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.Semaphore;
import com.nxp.ble.otalib.bleGlobalVariables.*;


/**
 * Created by afan on 2017/2/13.
 */

public class otaManager {
    private static final String TAG = otaManager.class.getSimpleName();
    BluetoothLeInterface mOtaIntf;
    private int mStartOffset = 0;
    private int mPercent = 0;
    private Semaphore semp = null;
    private final int mTimeout = 12;
    private final int mPacketSize = 256;
    private boolean mShouldStop = false;
    String mFilePath = null;
    int mByteRate = 0;
    int mElapsedTime = 0;
    bleGlobalVariables.otaResult mRetValue;
    Runnable updateRunnable;

    public otaManager() {
        this.mRetValue = bleGlobalVariables.otaResult.OTA_RESULT_SUCCESS;
        this.updateRunnable = new Runnable() {
            public void run() {
                otaManager.this.otaUpdateProcess(otaManager.this.mFilePath);
            }
        };
    }

    public void otaPrintBytes(byte[] bytes, String tag) {
        if(bytes != null) {
            StringBuilder stringBuilder = new StringBuilder(bytes.length);
            byte[] var7 = bytes;
            int var6 = bytes.length;

            for(int var5 = 0; var5 < var6; ++var5) {
                byte byteChar = var7[var5];
                stringBuilder.append(String.format("%02X ", new Object[]{Byte.valueOf(byteChar)}));
            }

            Log.i(TAG, tag + " :" + stringBuilder.toString());
        }
    }

    private byte cmdToValue(bleGlobalVariables.otaCmd cmd) {
        switch(cmd.ordinal()) {
            case 1:
                return (byte)1;
            case 2:
                return (byte)2;
            case 3:
                return (byte)3;
            case 4:
                return (byte)4;
            default:
                return (byte)0;
        }
    }

    private otaCmd valueToCmd(int val) {
        switch(val & 255) {
            case 1:
                return otaCmd.OTA_CMD_META_DATA;
            case 2:
                return otaCmd.OTA_CMD_BRICK_DATA;
            case 3:
                return otaCmd.OTA_CMD_DATA_VERIFY;
            case 4:
                return otaCmd.OTA_CMD_EXECUTION_NEW_CODE;
            default:
                return null;
        }
    }

    private boolean otaWrite(byte[] data) throws InterruptedException {
        if(this.shouldStopUpdate()) {
            Log.e(TAG, "otaWrite:Stopped for some reason");
            return false;
        } else if(!this.mOtaIntf.writeCharacteristic(data)) {
            Log.e(TAG, "Failed to write characteristic");
            return false;
        } else {
            return this.waitWriteDataCompleted();
        }
    }

    private boolean otaSendPacket(otaCmd cmd, short checksum, byte[] data, int dataLength) {
        Log.i(TAG, "otaSendPacket");
        byte cmdVal = this.cmdToValue(cmd);
        byte[] checksumBytes = new byte[]{(byte)checksum, (byte)(checksum >> 8)};
        byte[] head = new byte[3];
        int packetLength;
        byte[] dataPacket;
        switch(cmd.ordinal()) {
            case 1:
            case 2:
                head[0] = (byte)(dataLength + 1);
                head[1] = (byte)(dataLength + 1 >> 8);
                head[2] = cmdVal;
                packetLength = head.length + dataLength + checksumBytes.length;
                dataPacket = new byte[packetLength];
                System.arraycopy(head, 0, dataPacket, 0, head.length);
                System.arraycopy(data, 0, dataPacket, head.length, dataLength);
                System.arraycopy(checksumBytes, 0, dataPacket, head.length + dataLength, checksumBytes.length);
                break;
            case 3:
            case 4:
                packetLength = head.length + checksumBytes.length;
                dataPacket = new byte[packetLength];
                dataPacket[0] = 1;
                dataPacket[1] = 0;
                dataPacket[2] = cmdVal;
                dataPacket[3] = checksumBytes[0];
                dataPacket[4] = checksumBytes[1];
                break;
            default:
                Log.e(TAG, "otaSendPacket:unknown cmd type");
                return false;
        }

        int left = packetLength;

        int tempLen;
        for(byte BytesEachTime = 20; left > 0; left -= tempLen) {
            if(left > BytesEachTime) {
                tempLen = BytesEachTime;
            } else {
                tempLen = left;
            }

            byte[] tempPacket = new byte[tempLen];
            System.arraycopy(dataPacket, packetLength - left, tempPacket, 0, tempLen);

            try {
                if(!this.otaWrite(tempPacket)) {
                    return false;
                }
            } catch (InterruptedException var15) {
                var15.printStackTrace();
            }
        }

        return true;
    }

    private int otaSendMetaData(FileInputStream fin) throws IOException {
        Log.i(TAG, "otaSendMetaData");
        boolean ret = true;
        byte[] metaLen = new byte[2];
        fin.read(metaLen);
        short dataLength = (short)(((metaLen[1] & 255) << 8) + (metaLen[0] & 255));
        byte[] data = new byte[dataLength];
        int var8 = fin.read(data);
        if(var8 < 0) {
            return -1;
        } else {
            short checksum = this.cmdToValue(otaCmd.OTA_CMD_META_DATA);

            for(int i = 0; i < var8; ++i) {
                checksum = (short)(checksum + (data[i] & 255));
            }

            return this.otaSendPacket(otaCmd.OTA_CMD_META_DATA, checksum, data, dataLength)?var8 + 2:-1;
        }
    }

    private int otaSendBrickData(FileInputStream fin, int dataLength) throws IOException {
        boolean ret = true;
        byte[] data = new byte[dataLength];
        int var7 = fin.read(data);
        if(var7 <= 0) {
            Log.w(TAG, "otaSendBrickData:No data read from file");
            return -1;
        } else {
            if(var7 < dataLength) {
                dataLength = var7;
            }

            short checksum = this.cmdToValue(otaCmd.OTA_CMD_BRICK_DATA);

            for(int i = 0; i < dataLength; ++i) {
                checksum = (short)(checksum + (data[i] & 255));
            }

            if(this.otaSendPacket(otaCmd.OTA_CMD_BRICK_DATA, checksum, data, dataLength)) {
                return var7;
            } else {
                Log.e(TAG, "otaSendBrickData:failed to send packet");
                return -2;
            }
        }
    }

    private boolean otaSendVerifyCmd() {
        byte checksum = this.cmdToValue(otaCmd.OTA_CMD_DATA_VERIFY);
        return this.otaSendPacket(otaCmd.OTA_CMD_DATA_VERIFY, checksum, (byte[])null, 0) && this.waitVerifyCmdDone();
    }

    private void otaSendResetCmd() {
        byte checksum = this.cmdToValue(otaCmd.OTA_CMD_EXECUTION_NEW_CODE);
        this.otaSendPacket(otaCmd.OTA_CMD_EXECUTION_NEW_CODE, checksum, (byte[])null, 0);
    }

    private void releaseSemaphore(Semaphore semp) {
        semp.release();
    }

    private boolean waitSemaphore(Semaphore semp) {
        int i = 0;

        do {
            if(i++ >= 12000) {
                return false;
            }

            boolean getAccquire = semp.tryAcquire();
            if(getAccquire) {
                return true;
            }

            try {
                Thread.sleep(1L);
            } catch (InterruptedException var5) {
                var5.printStackTrace();
            }
        } while(!this.shouldStopUpdate());

        return false;
    }

    private void setOffset(int offset) {
        this.mStartOffset = offset;
        this.releaseSemaphore(this.semp);
    }

    private int getOffset() {
        return this.waitSemaphore(this.semp)?this.mStartOffset:-1;
    }

    private void notifyVerifyCmdDone() {
        this.releaseSemaphore(this.semp);
    }

    private boolean waitVerifyCmdDone() {
        return this.waitSemaphore(this.semp);
    }

    public void notifyWriteDataCompleted() {
        this.releaseSemaphore(this.semp);
    }

    private boolean waitWriteDataCompleted() {
        return this.waitSemaphore(this.semp);
    }

    private void notifyReadDataCompleted() {
        this.releaseSemaphore(this.semp);
    }

    private boolean waitReadDataCompleted() {
        return this.waitSemaphore(this.semp);
    }

    public void otaGetResult(byte[] notify_data) {
        otaCmd cmdType = this.valueToCmd(notify_data[2] & 255);
        if(cmdType == null) {
            this.otaPrintBytes(notify_data, "Notify data: ");
            this.serErrorCode(otaResult.OTA_RESULT_RECEIVED_INVALID_PACKET);
        } else {
            switch(notify_data[3]) {
                case 0:
                    this.serErrorCode(otaResult.OTA_RESULT_SUCCESS);
                    break;
                case 1:
                    this.serErrorCode(otaResult.OTA_RESULT_PKT_CHECKSUM_ERROR);
                    break;
                case 2:
                    this.serErrorCode(otaResult.OTA_RESULT_PKT_LEN_ERROR);
                    break;
                case 3:
                    this.serErrorCode(otaResult.OTA_RESULT_DEVICE_NOT_SUPPORT_OTA);
                    break;
                case 4:
                    this.serErrorCode(otaResult.OTA_RESULT_FW_SIZE_ERROR);
                    break;
                case 5:
                    this.serErrorCode(otaResult.OTA_RESULT_FW_VERIFY_ERROR);
                    break;
                default:
                    this.serErrorCode(otaResult.OTA_RESULT_INVALID_ARGUMENT);
            }

            if(this.mRetValue != otaResult.OTA_RESULT_SUCCESS) {
                this.otaPrintBytes(notify_data, "Notify data: ");
            } else {
                switch(cmdType.ordinal()) {
                    case 1:
                        short offset = (short)((notify_data[4] & 255) + ((notify_data[5] & 255) << 8));
                        this.setOffset(offset);
                        break;
                    case 2:
                        this.notifyReadDataCompleted();
                        break;
                    case 3:
                        this.notifyVerifyCmdDone();
                        Log.i(TAG, "OTA_CMD_DATA_VERIFY");
                        break;
                    case 4:
                        Log.i(TAG, "This should never happened");
                        break;
                    default:
                        Log.i(TAG, "Exit " + (notify_data[2] & 255));
                        this.serErrorCode(bleGlobalVariables.otaResult.OTA_RESULT_INVALID_ARGUMENT);
                        return;
                }

            }
        }
    }

    private boolean shouldStopUpdate() {
        return this.mShouldStop;
    }

    private void serErrorCode(bleGlobalVariables.otaResult ret) {
        this.mRetValue = ret;
    }

    public bleGlobalVariables.otaResult otaStart(String file, BluetoothLeInterface intf) {
        Log.i(TAG, "otaStart");
        if(!file.isEmpty() && intf != null) {
            this.mFilePath = file;
            this.mOtaIntf = intf;
            this.mShouldStop = false;
            this.mPercent = 0;
            this.mByteRate = 0;
            this.mElapsedTime = 0;
            this.semp = new Semaphore(0);
            intf.setCharacteristicNotification(true);
            Thread updateStart = new Thread(this.updateRunnable);
            updateStart.start();
            return bleGlobalVariables.otaResult.OTA_RESULT_SUCCESS;
        } else {
            Log.e(TAG, "otaUpdateInit:argument invalid");
            return bleGlobalVariables.otaResult.OTA_RESULT_INVALID_ARGUMENT;
        }
    }

    private void otaUpdateProcess(String filePath) {
        Log.i(TAG, "otaUpdateProcess");
        boolean offset = false;
        boolean ret = false;

        try {
            FileInputStream e = new FileInputStream(filePath);
            int fileSize = e.available();
            if(fileSize == 0) {
                e.close();
                this.serErrorCode(bleGlobalVariables.otaResult.OTA_RESULT_FW_SIZE_ERROR);
                return;
            }

            int metaSize = this.otaSendMetaData(e);
            if(metaSize < 0) {
                e.close();
                this.serErrorCode(bleGlobalVariables.otaResult.OTA_RESULT_SEND_META_ERROR);
                return;
            }

            int offset1 = this.getOffset();
            if(offset1 < 0) {
                Log.e(TAG, "wait cmd OTA_CMD_META_DATA timeout");
                e.close();
                this.serErrorCode(otaResult.OTA_RESULT_META_RESPONSE_TIMEOUT);
                return;
            }

            if(offset1 > 0) {
                e.skip((long)offset1);
            }

            int brickDataSize = fileSize - metaSize;
            int transfereedSize = 0;
            Log.d(TAG, "offset=" + offset1 + " meta size " + metaSize);
            long begin = Calendar.getInstance().getTimeInMillis();

            do {
                int ret1 = this.otaSendBrickData(e, 256);
                if(ret1 < 0) {
                    e.close();
                    Log.e(TAG, "otaUpdateProcess Exit for some transfer issue");
                    this.serErrorCode(otaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
                    return;
                }

                if(!this.waitReadDataCompleted()) {
                    Log.e(TAG, "waitReadDataCompleted timeout");
                    this.serErrorCode(otaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
                    return;
                }

                offset1 += ret1;
                this.mPercent = offset1 * 100 / fileSize;
                transfereedSize += 256;
                long now = Calendar.getInstance().getTimeInMillis();
                this.mElapsedTime = (int)((now - begin) / 1000L);
                this.mByteRate = (int)((long)(transfereedSize * 1000) / (now - begin));
            } while(offset1 < brickDataSize);

            if(!this.otaSendVerifyCmd()) {
                e.close();
                this.serErrorCode(bleGlobalVariables.otaResult.OTA_RESULT_FW_VERIFY_ERROR);
                return;
            }

            this.mPercent = 100;
            this.otaSendResetCmd();
            e.close();
        } catch (Exception var13) {
            var13.printStackTrace();
        }

        Log.i(TAG, "otaUpdateProcess Exit");
        this.serErrorCode(bleGlobalVariables.otaResult.OTA_RESULT_SUCCESS);
    }

    public bleGlobalVariables.otaResult otaGetProcess(int[] extra) {
        if(extra.length < 8) {
            Log.e(TAG, "buffer is too small,at least 8 intgent");
            return bleGlobalVariables.otaResult.OTA_RESULT_INVALID_ARGUMENT;
        } else {
            Arrays.fill(extra, 0);
            extra[0] = this.mPercent;
            extra[1] = this.mByteRate;
            extra[2] = this.mElapsedTime;
            return this.mRetValue;
        }
    }

    public void otaStop() {
        this.mShouldStop = true;
    }
}
