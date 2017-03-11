package com.nxp.ble.meshlib.callback;

public abstract interface IProvisionCallback
{
  public static final int PROVISION_FAILED_ERROR_PARAMETERS = 1;
  public static final int PROVISION_FAILED_DATABASE_FULL = 2;
  public static final int PROVISION_FAILED_CANNOT_CONNECT = 3;
  public static final int PROVISION_FAILED_DISCONNECT_ACCIDENTLY = 4;
  public static final int PROVISION_FAILED_OTHER = 5;
  
  public abstract void onTimeout();
  
  public abstract void onSuccess();
  
  public abstract void onFailed(int paramInt);
}
