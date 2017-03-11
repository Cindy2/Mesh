package com.beesmart.blemesh;

import java.util.Locale;

public class Constants {
	
	public static String EMPTY = "";
	public static String SPACE = " ";
	public static String COMMA = ",";
	public static String PROGRESS = "...";
	
	public static String TITLE = "TITLE";
	public static String IMAGE = "IMAGE";
	public static String VERSION = "VERSION";
	
	public static String CONTROL_TYPE = "CONTROL_TYPE";
	public static int CONTROL_TYPE_DEVICE = 0;
	public static int CONTROL_TYPE_GROUP = 1;
	public static String CONTROL_DEST = "CONTROL_DEST";
	public static String CONTROL_NAME = "CONTROL_NAME";
	public static String CONTROL_STATUS = "CONTROL_STATUS";
	
	public static int JOIN_TIMEOUT = 10000;
	public static int SCAN_PERIOD = 15000;
	
	public static String MY_DATA = "my_data";
	public static String KEY_SETTING_FLAG = "key_setting_flag";
	public static String KEY = "key";
	public static String GROUP_NAME_COUNT = "group_name_count";
	public static String DEVICE_NAME_COUNT = "device_name_count";
	public static String SWITCH_NAME_COUNT = "switch_name_count";
	public static String NETWORK_FLAG = "network_flag";
	public static int NETWORK_FLAG_CERATE = 1;
	public static int NETWORK_FLAG_NONE = 0;
	public static int NETWORK_FLAG_JOIN = 2;

	public static int CCTW_COLOR_MAX = 6500;//the max color of CCTW blub
	public static int CCTW_COLOR_MIN = 2700;//the max color of CCTW blub
	public static int MAX_SELECTED_GROUP = 2;
	
	public static String printHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			result += hex.toUpperCase(Locale.US);
		}
		return result;
	}
	
	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
