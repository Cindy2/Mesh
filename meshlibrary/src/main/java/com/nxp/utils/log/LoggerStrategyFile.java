/*     */ package com.nxp.utils.log;
/*     */ 
/*     */ import android.os.Environment;
/*     */ import android.util.Log;
/*     */ import java.io.File;
/*     */ import java.io.FileWriter;
/*     */ import java.io.IOException;
/*     */ import java.text.MessageFormat;
/*     */ import java.util.Date;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class LoggerStrategyFile
/*     */   implements ILoggerStrategy
/*     */ {
/*     */   private FileWriter writer;
/*  28 */   private final String[] levels = { "Verbose", "Debug", "Info", "Warn", "Error" };
/*     */   
/*     */ 
/*     */   public LoggerStrategyFile()
/*     */   {
/*     */     try
/*     */     {
/*  35 */       String fname = "logFile" + new Date().getTime() + ".txt";
/*  36 */       File fTemp = Environment.getExternalStorageDirectory();
/*  37 */       Log.d("Logger", "Checking writability at " + fTemp.getAbsolutePath());
/*     */       
/*  39 */       if (fTemp.canWrite()) {
/*  40 */         Log.d("Logger", "Can write");
/*     */         
/*  42 */         fTemp = new File(fTemp, "nblog");
/*  43 */         if (fTemp.isFile()) {
/*  44 */           fTemp.delete();
/*     */         }
/*  46 */         if (!fTemp.exists()) {
/*  47 */           fTemp.mkdir();
/*     */         }
/*     */         
/*  50 */         fTemp = new File(fTemp, fname);
/*  51 */         this.writer = new FileWriter(fTemp, true);
/*     */       } else {
/*  53 */         Log.d("Logger", "Can not write");
/*     */       }
/*     */     } catch (IOException e) {
/*  56 */       e.printStackTrace();
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public void dispose()
/*     */   {
/*  64 */     if (this.writer != null) {
/*     */       try {
/*  66 */         this.writer.close();
/*  67 */         this.writer = null;
/*     */       } catch (IOException e) {
/*  69 */         e.printStackTrace();
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public void i(String tag, String message) {
/*  75 */     write(4, tag, message);
/*     */   }
/*     */   
/*     */   public void w(String tag, String message) {
/*  79 */     write(5, tag, message);
/*     */   }
/*     */   
/*     */   public void d(String tag, String message) {
/*  83 */     write(3, tag, message);
/*     */   }
/*     */   
/*     */   public void e(String tag, String message) {
/*  87 */     write(6, tag, message);
/*     */   }
/*     */   
/*     */   public void v(String tag, String message) {
/*  91 */     write(2, tag, message);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void n(String tag, String message) {}
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private String formatDate(Date date)
/*     */   {
/* 106 */     return MessageFormat.format("{0,date} {0,time}", new Object[] { date });
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private String levelToString(int level)
/*     */   {
/* 117 */     return this.levels[(level - 2)];
/*     */   }
/*     */   
/*     */   public void write(int level, String tag, String message) {
/* 121 */     writeImpl(level, tag, message);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void writeImpl(int level, String tag, String message)
/*     */   {
/* 135 */     if (this.writer != null) {
/*     */       try {
/* 137 */         this.writer.write("[" + formatDate(new Date()) + "] " + levelToString(level) + " [" + tag + "] " + message + 
/* 138 */           "\r\n");
/* 139 */         this.writer.flush();
/*     */       } catch (IOException e) {
/* 141 */         e.printStackTrace();
/*     */       }
/*     */     }
/* 144 */     Log.println(level, tag, message);
/*     */   }
/*     */ }

