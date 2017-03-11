package com.nxp.ble.meshlib.callback;

public abstract interface ILeaveCallback
{
  public static final int LEAVE_FAILED = 1;
  
  public abstract void onSuccess();
  
  public abstract void onFailed(int paramInt);
}

