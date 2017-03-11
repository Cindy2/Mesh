package com.nxp.ble.meshlib.callback;


import com.nxp.ble.meshlib.command.base.ICommandCallback;

public abstract interface ICommandResponseCallback
        extends ICommandCallback {
    public abstract void onSuccess();

    public abstract void onFailed();
}

