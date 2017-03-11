package com.nxp.ble.meshlib.operation;

import android.content.Context;

import com.nxp.ble.meshlib.MeshService;
import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.callback.IOTAResponseCallback;
import com.nxp.ble.meshlib.command.DeleteDeviceInGroupCommand;
import com.nxp.ble.meshlib.command.DeleteDeviceInMeshCommand;
import com.nxp.ble.meshlib.command.ResetGroupsCommand;
import com.nxp.ble.meshlib.command.StartOTACommand;


public class OperationInMesh {
    private Context mContext;
    private MeshService mService;

    public OperationInMesh(Context context, MeshService service) {
        this.mContext = context;
        this.mService = service;
    }

    public void deleteDevice(int deviceId, int flag, ICommandResponseCallback callback) {
        this.mService.sendCommand(new DeleteDeviceInMeshCommand(this.mContext, deviceId, flag, callback));
    }

    public void removeDevice(int deviceId, int groupId, int flag, ICommandResponseCallback callback) {
        this.mService.sendCommand(new DeleteDeviceInGroupCommand(this.mContext, deviceId, groupId, flag, callback));
    }

    public void editGroups(int deviceId, int[] groupsId, int flag, ICommandResponseCallback callback) {
        this.mService.sendCommand(new ResetGroupsCommand(this.mContext, deviceId, groupsId, flag, callback));
    }

    public void startOTA(IOTAResponseCallback callback) {
        this.mService.sendCommand(new StartOTACommand(this.mContext, callback));
    }
}
