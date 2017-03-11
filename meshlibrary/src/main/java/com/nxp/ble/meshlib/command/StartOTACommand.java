package com.nxp.ble.meshlib.command;

import android.content.Context;

import com.nxp.ble.meshlib.callback.IOTAResponseCallback;
import com.nxp.ble.meshlib.command.base.AbstractCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;

public class StartOTACommand extends AbstractCommand {
    public StartOTACommand(Context context, IOTAResponseCallback callback) {
        super(callback);
        byte[] cmd = new byte[]{(byte) 8, (byte) 0, (byte) 0, (byte) 2, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        cmd[4] = (byte) this.getId();
        CSLog.i(StartOTACommand.class, "StartOTACommand: " + CryptoUtils.toHex(cmd));

        try {
            String e = PrivateData.getProvisionKey(context);
            byte[] encryptcmd = CryptoUtils.encryptByte(CryptoUtils.toByte(e), cmd);
            this.setCommandValue(encryptcmd);
        } catch (Exception var6) {
            var6.printStackTrace();
        }

    }

    public void doResponse(byte[] value) {
        if (value != null && value.length >= 6) {
            int mOTACallback2 = value[5] & 255;
            IOTAResponseCallback mOTACallback1 = (IOTAResponseCallback) this.getCommandCallback();
            if (mOTACallback1 != null) {
                if (mOTACallback2 == 2) {
                    int nodeid = value[2] & 255;
                    mOTACallback1.onSuccess(nodeid);
                } else {
                    mOTACallback1.onFailed();
                }
            }
        } else {
            IOTAResponseCallback mOTACallback = (IOTAResponseCallback) this.getCommandCallback();
            if (mOTACallback != null) {
                mOTACallback.onFailed();
            }
        }

    }
}
