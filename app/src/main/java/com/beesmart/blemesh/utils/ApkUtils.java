package com.beesmart.blemesh.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ApkUtils {
	/**
	 * 获取Apk的版本名称
	 * @param mContext
	 * @return
	 */
	public static String getApkVersionName(Context mContext){
		try {
			return mContext.getApplicationContext().getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 *获取apk的版本号
	 * @param mContext
	 * @return
	 */
	public static int getApkVersionCode(Context mContext){
		try {
			return mContext.getApplicationContext().getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * 判断App是否过期
	 * @param act
     */
	public static void checkAppStatus(Activity act) {
		String format = "yyyy-MM-dd HH:mm:ss";
		String date = "2016-07-07 23:59:59";
		SimpleDateFormat sf = new SimpleDateFormat(format);
		try {
			if(sf.parse(date).getTime() < System.currentTimeMillis()){//过期
				act.finish();
				System.exit(0);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
