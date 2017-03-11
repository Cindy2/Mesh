package com.nxp.ble.meshlib.callback;

public abstract interface IUpdateStatusCallback
{
  public static final int DISCONNECT_ACCIDENTLY = 1;
  
  public abstract void onDisconnect(int paramInt);
  
  public abstract void onUpdate(int paramInt1, int paramInt2);
}
