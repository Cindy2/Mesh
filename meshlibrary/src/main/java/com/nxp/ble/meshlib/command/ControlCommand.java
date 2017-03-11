package com.nxp.ble.meshlib.command;

import android.content.Context;

import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.command.base.AbstractCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;


public class ControlCommand extends AbstractCommand {
    public ControlCommand(Context context, byte[] cmd, ICommandResponseCallback callback) {
        super(callback);

        cmd[4] = ((byte) getId());

        CSLog.i(ControlCommand.class, "Control cmd: " + CryptoUtils.toHex(cmd));
        try {
            String key = PrivateData.getProvisionKey(context);
            byte[] encryptcmd = CryptoUtils.encryptByte(CryptoUtils.toByte(key), cmd);

            setCommandValue(encryptcmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void doResponse(byte[] value) {
        CSLog.i(ControlCommand.class, "Response cmd: " + CryptoUtils.toHex(value));

        if ((value != null) && (value.length > 5)) {
            int result = value[5] & 0xFF;
            ICommandResponseCallback responseCallback = (ICommandResponseCallback) getCommandCallback();

            if (responseCallback != null) {
                if (result == 0) {
                    responseCallback.onSuccess();
                } else {
                    responseCallback.onFailed();
                }
            }
        } else {
            ICommandResponseCallback responseCallback = (ICommandResponseCallback) getCommandCallback();

            if (responseCallback != null) {
                responseCallback.onFailed();
            }
        }
    }
}

