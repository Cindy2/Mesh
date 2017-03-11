package com.nxp.ble.meshlib.callback;

public abstract interface IJoinCallback
{
  public static final int JOIN_FAILED_CANNOT_FIND_NODE = 1;
  public static final int JOIN_FAILED_DISCONNECT_ACCIDENTLY = 2;
  public static final int JOIN_FAILED_OTHER = 3;
  
  public abstract void onTimeout();
  
  public abstract void onSuccess();
  
  public abstract void onFailed(int paramInt);
  
  public abstract void onUpdate(int paramInt1, int paramInt2);
}
