//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.nxp.ble.meshlib.command;

import android.content.Context;

import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.command.base.AbstractCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceAffiliationPO;
import com.nxp.utils.po.DeviceInfoPO;


public class DeleteDeviceInGroupCommand extends AbstractCommand {
    private int mNodeId;
    private int mGroupId;

    public DeleteDeviceInGroupCommand(Context context, int deviceId, int groupId, int flag, ICommandResponseCallback callback) {
        super(callback);
        this.mNodeId = deviceId;
        this.mGroupId = groupId;
        byte[] cmd = new byte[]{(byte)6, (byte)flag, (byte)1, (byte)0, (byte)1, (byte)2, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        cmd[2] = (byte)this.mNodeId;
        cmd[4] = (byte)this.getId();
        DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(deviceId);
        int[] groups = deviceInfoPO.getGroupAffiliations();
        int j = 0;
        int[] var13 = groups;
        int var12 = groups.length;

        int e;
        int encryptcmd;
        for(encryptcmd = 0; encryptcmd < var12; ++encryptcmd) {
            e = var13[encryptcmd];
            if(e != this.mGroupId) {
                cmd[6 + j] = (byte)e;
                ++j;
            }
        }

        cmd[3] = 6;
        j = 0;
        var13 = groups;
        var12 = groups.length;

        for(encryptcmd = 0; encryptcmd < var12; ++encryptcmd) {
            e = var13[encryptcmd];
            cmd[8 + j] = (byte)e;
            ++j;
        }

        CSLog.i(DeleteDeviceInGroupCommand.class, "delete child cmd: " + CryptoUtils.toHex(cmd));

        try {
            String var15 = PrivateData.getProvisionKey(context);
            byte[] var16 = CryptoUtils.encryptByte(CryptoUtils.toByte(var15), cmd);
            this.setCommandValue(var16);
        } catch (Exception var14) {
            var14.printStackTrace();
        }

    }

    public void doResponse(byte[] value) {
        if(value != null && value.length > 6) {
            int mDeleteChildCallback2 = value[6] & 255;
            ICommandResponseCallback mDeleteChildCallback1 = (ICommandResponseCallback)this.getCommandCallback();
            if(mDeleteChildCallback1 != null) {
                if(mDeleteChildCallback2 == 0) {
                    DeviceAffiliationPO.getDeviceAffiliationPO(this.mGroupId, this.mNodeId).remove();
                    mDeleteChildCallback1.onSuccess();
                } else {
                    mDeleteChildCallback1.onFailed();
                }
            }
        } else {
            ICommandResponseCallback mDeleteChildCallback = (ICommandResponseCallback)this.getCommandCallback();
            if(mDeleteChildCallback != null) {
                mDeleteChildCallback.onFailed();
            }
        }

    }
}
