package com.nxp.ble.meshlib;

import android.content.Context;

import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.command.ControlCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.log.CSLog;


public class CCTWUtil {
    private Context mContext;
    private MeshService mMeshService;

    public CCTWUtil(Context context, MeshService service) {
        this.mContext = context;
        this.mMeshService = service;
    }

    public void changeDeviceParameters(int deviceId, boolean checked, byte intensity, byte color, int flag, ICommandResponseCallback callback) {
        byte[] cmd = new byte[]{(byte)2, (byte)flag, (byte)deviceId, (byte)4, (byte)0, (byte)4, (byte)1, (byte)40, (byte)50, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        cmd[6] = (byte)(checked?1:0);
        cmd[7] = color;
        cmd[8] = intensity;
        CSLog.i(CCTWUtil.class, "change device: " + CryptoUtils.toHex(cmd));
        this.mMeshService.sendCommand(new ControlCommand(this.mContext, cmd, callback));
    }

    public void changeDeviceParameters(int deviceId, boolean checked, int flag, ICommandResponseCallback callback) {
        byte[] cmd = new byte[]{(byte)2, (byte)flag, (byte)deviceId, (byte)4, (byte)0, (byte)4, (byte)1, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        cmd[6] = (byte)(checked?1:0);
        CSLog.i(CCTWUtil.class, "change device: " + CryptoUtils.toHex(cmd));
        this.mMeshService.sendCommand(new ControlCommand(this.mContext, cmd, callback));
    }

    public void changeGroupParameters(int groupId, boolean checked, byte intensity, byte color, int flag) {
        byte[] cmd = new byte[]{(byte)4, (byte)flag, (byte)groupId, (byte)4, (byte)0, (byte)4, (byte)1, (byte)40, (byte)50, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        cmd[6] = (byte)(checked?1:0);
        cmd[7] = color;
        cmd[8] = intensity;
        CSLog.i(CCTWUtil.class, "change group: " + CryptoUtils.toHex(cmd));
        this.mMeshService.sendCommandWithoutCallback(cmd);
    }

    public void changeGroupParameters(int groupId, boolean checked, int flag) {
        byte[] cmd = new byte[]{(byte)4, (byte)flag, (byte)groupId, (byte)4, (byte)0, (byte)4, (byte)1, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        cmd[6] = (byte)(checked?1:0);
        CSLog.i(CCTWUtil.class, "change group: " + CryptoUtils.toHex(cmd));
        this.mMeshService.sendCommandWithoutCallback(cmd);
    }

    public void changeDeviceBlink(int deviceId, int flag) {
        byte[] cmd = new byte[]{(byte)2, (byte)flag, (byte)deviceId, (byte)4, (byte)0, (byte)4, (byte)2, (byte)40, (byte)50, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        CSLog.i(CCTWUtil.class, "changeDeviceBlink: " + CryptoUtils.toHex(cmd));
        this.mMeshService.sendCommandWithoutCallback(cmd);
    }
}

