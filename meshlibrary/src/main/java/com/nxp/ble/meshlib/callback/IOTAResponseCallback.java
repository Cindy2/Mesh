package com.nxp.ble.meshlib.callback;


import com.nxp.ble.meshlib.command.base.ICommandCallback;

public abstract interface IOTAResponseCallback
        extends ICommandCallback {
    public abstract void onSuccess(int paramInt);

    public abstract void onFailed();
}

