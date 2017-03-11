/*    */ package com.nxp.utils.log;
/*    */ 
/*    */ import android.util.Log;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class LoggerStrategyLog
/*    */   implements ILoggerStrategy
/*    */ {
/*    */   public void i(String tag, String message)
/*    */   {
/* 19 */     Log.i(tag, message);
/*    */   }
/*    */   
/*    */   public void w(String tag, String message) {
/* 23 */     Log.w(tag, message);
/*    */   }
/*    */   
/*    */   public void d(String tag, String message) {
/* 27 */     Log.d(tag, message);
/*    */   }
/*    */   
/*    */   public void e(String tag, String message) {
/* 31 */     Log.e(tag, message);
/*    */   }
/*    */   
/*    */   public void v(String tag, String message) {
/* 35 */     Log.v(tag, message);
/*    */   }
/*    */   
/*    */ 
/*    */   public void n(String tag, String message) {}
/*    */   
/*    */   public void write(int level, String tag, String message)
/*    */   {
/* 43 */     Log.println(level, tag, message);
/*    */   }
/*    */ }
