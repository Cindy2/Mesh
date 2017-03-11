package com.nxp.ble.meshlib;

import android.content.Context;

import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.command.ControlCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.log.CSLog;

public class BuzzerUtil {
    private Context mContext;
    private MeshService mMeshService;

    public BuzzerUtil(Context context, MeshService service) {
        this.mContext = context;
        this.mMeshService = service;
    }

    public void changeDeviceParameters(int deviceId, int flag, ICommandResponseCallback callback) {
        byte[] cmd = new byte[]{(byte)2, (byte)flag, (byte)deviceId, (byte)1, (byte)0, (byte)2, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        CSLog.i(BuzzerUtil.class, "change device: " + CryptoUtils.toHex(cmd));
        this.mMeshService.sendCommand(new ControlCommand(this.mContext, cmd, callback));
    }

    public void changeGroupParameters(int groupId, int flag) {
        byte[] cmd = new byte[]{(byte)4, (byte)flag, (byte)groupId, (byte)1, (byte)0, (byte)2, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        CSLog.i(BuzzerUtil.class, "change group: " + CryptoUtils.toHex(cmd));
        this.mMeshService.sendCommandWithoutCallback(cmd);
    }
}

