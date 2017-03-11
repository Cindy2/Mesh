/*    */ package com.nxp.utils.log;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class Logger
/*    */ {
/* 13 */   private static ILoggerStrategy logger = new LoggerStrategyLog();
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static void i(String tag, String message)
/*    */   {
/* 24 */     logger.i(tag, message);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static void w(String tag, String message)
/*    */   {
/* 36 */     logger.w(tag, message);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static void d(String tag, String message)
/*    */   {
/* 48 */     logger.d(tag, message);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static void e(String tag, String message)
/*    */   {
/* 60 */     logger.e(tag, message);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static void v(String tag, String message)
/*    */   {
/* 72 */     logger.v(tag, message);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   public static void n(String tag, String message)
/*    */   {
/* 84 */     logger.n(tag, message);
/*    */   }
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
/*    */   public static void write(int level, String tag, String message)
/*    */   {
/* 98 */     logger.write(level, tag, message);
/*    */   }
/*    */ }
