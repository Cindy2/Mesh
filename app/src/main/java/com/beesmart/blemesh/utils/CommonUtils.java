package com.beesmart.blemesh.utils;

import com.beesmart.blemesh.Constants;

/**
 * Created by alphawong on 2016/5/31.
 */
public class CommonUtils {
    public static String getCCTWColor(int colorInt){
        return (colorInt * (Constants.CCTW_COLOR_MAX-Constants.CCTW_COLOR_MIN))/100 + 2700 +"K";
    }
}
