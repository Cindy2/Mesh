package com.nxp.utils.log;

public abstract interface ILoggerStrategy
{
  public abstract void i(String paramString1, String paramString2);
  
  public abstract void w(String paramString1, String paramString2);
  
  public abstract void d(String paramString1, String paramString2);
  
  public abstract void e(String paramString1, String paramString2);
  
  public abstract void v(String paramString1, String paramString2);
  
  public abstract void n(String paramString1, String paramString2);
  
  public abstract void write(int paramInt, String paramString1, String paramString2);
}
