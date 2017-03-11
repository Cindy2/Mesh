package com.nxp.ble.meshlib.command;

import android.content.Context;

import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.command.base.AbstractCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceInfoPO;

public class DeleteDeviceInMeshCommand extends AbstractCommand {
    private int mNodeId;

    public DeleteDeviceInMeshCommand(Context context, int deviceId, int flag, ICommandResponseCallback callback) {
        super(callback);
        this.mNodeId = deviceId;
        byte[] cmd = new byte[]{(byte) 6, (byte) flag, (byte) 1, (byte) 2, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        cmd[2] = (byte) deviceId;
        cmd[4] = (byte) this.getId();
        CSLog.i(DeleteDeviceInMeshCommand.class, "delete child cmd: " + CryptoUtils.toHex(cmd));

        try {
            String e = PrivateData.getProvisionKey(context);
            byte[] encryptcmd = CryptoUtils.encryptByte(CryptoUtils.toByte(e), cmd);
            this.setCommandValue(encryptcmd);
        } catch (Exception var8) {
            var8.printStackTrace();
        }

    }

    public void doResponse(byte[] value) {
        if (value != null && value.length > 6) {
            int mDeleteChildCallback2 = value[6] & 255;
            ICommandResponseCallback mDeleteChildCallback1 = (ICommandResponseCallback) this.getCommandCallback();
            if (mDeleteChildCallback1 != null) {
                if (mDeleteChildCallback2 == 0) {
                    DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(this.mNodeId);
                    deviceInfoPO.remove();
                    mDeleteChildCallback1.onSuccess();
                } else {
                    mDeleteChildCallback1.onFailed();
                }
            }
        } else {
            ICommandResponseCallback mDeleteChildCallback = (ICommandResponseCallback) this.getCommandCallback();
            if (mDeleteChildCallback != null) {
                mDeleteChildCallback.onFailed();
            }
        }

    }
}