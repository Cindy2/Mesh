package com.nxp.ble.meshlib.command;

import android.content.Context;

import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.command.base.AbstractCommand;
import com.nxp.utils.crypto.CryptoUtils;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceAffiliationPO;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.GroupInfoPO;


public class ResetGroupsCommand extends AbstractCommand {
    private int mNodeId;
    private int[] mGroupsId;

    public ResetGroupsCommand(Context context, int deviceId, int[] groupsId, int flag, ICommandResponseCallback callback) {
        super(callback);
        this.mNodeId = deviceId;
        this.mGroupsId = groupsId;
        byte[] cmd = new byte[]{(byte)6, (byte)flag, (byte)1, (byte)0, (byte)1, (byte)2, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        cmd[2] = (byte)deviceId;
        cmd[3] = 6;
        cmd[4] = (byte)this.getId();

        for(int deviceInfoPO = 0; deviceInfoPO < groupsId.length; ++deviceInfoPO) {
            cmd[6 + deviceInfoPO] = (byte)this.mGroupsId[deviceInfoPO];
        }

        DeviceInfoPO var12 = DeviceInfoPO.getDeviceInfoPO(deviceId);
        int[] originId = var12.getGroupAffiliations();

        for(int e = 0; e < originId.length; ++e) {
            cmd[8 + e] = (byte)originId[e];
        }

        CSLog.i(ResetGroupsCommand.class, "reset groups cmd: " + CryptoUtils.toHex(cmd));

        try {
            String var13 = PrivateData.getProvisionKey(context);
            byte[] encryptcmd = CryptoUtils.encryptByte(CryptoUtils.toByte(var13), cmd);
            this.setCommandValue(encryptcmd);
        } catch (Exception var11) {
            var11.printStackTrace();
        }

    }

    public void doResponse(byte[] value) {
        if(value != null && value.length > 6) {
            int var8 = value[6] & 255;
            ICommandResponseCallback mDeleteChildCallback1 = (ICommandResponseCallback)this.getCommandCallback();
            if(mDeleteChildCallback1 != null) {
                if(var8 == 0) {
                    DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(this.mNodeId);
                    deviceInfoPO.removeAffiliations();

                    for(int i = 0; i < this.mGroupsId.length; ++i) {
                        if(this.mGroupsId[i] > 0) {
                            DeviceAffiliationPO deviceAffiliationPO = DeviceAffiliationPO.createAffiliationPO(this.mGroupsId[i], deviceInfoPO.getNodeId());
                            deviceAffiliationPO.save();
                            GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(this.mGroupsId[i]);
                            if(groupInfoPO != null) {
                                groupInfoPO.setGroupType(deviceInfoPO.getDeviceType());
                                groupInfoPO.save();
                            }
                        }
                    }

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

