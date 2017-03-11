package com.beesmart.blemesh.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.beesmart.blemesh.customwidgets.CCTWViewHolder;

/**
 * Created by alphawong on 2016/3/30.
 * 通知者模式
 */
public class DeviceStatusObserver {

    static List<Observer> observers = new ArrayList<>();

    public static void subscribe(Observer observer) {

//        if (!isRegister(observer)){
            observers.add(observer);
            Log.e("DeviceStatusObserver ","Observers after added size is >>>>:"+observers.size());
//        }
    }
    public static boolean isRegister(Observer observer){
        CCTWViewHolder observer1 = (CCTWViewHolder) observer;
        for (Observer it:observers) {
            CCTWViewHolder viewHolder = (CCTWViewHolder) it;
            if (viewHolder.relativeId == observer1.relativeId&&viewHolder.type == observer1.type){
                return true;
            }
        }
        return false;
    }
    public static void unsubcribe(Observer observer){
        observers.remove(observer);
        Log.e("DeviceStatusObserver ","Observers after removed size is >>>>:"+observers.size());
    }
    public static void unsubcribeGroupDevice(){
        List<Observer> temp = new ArrayList<>();

        for (Observer observer : observers) {
            if (((CCTWViewHolder) observer).controlType == CCTWViewHolder.CONTROL_IN_GROUP) {
//                observers.remove(observer);
                temp.add(observer);
            }
        }
        for (int i = 0; i < temp.size(); i++) {
            observers.remove(temp.get(i));
        }
        temp.clear();
        Log.e("DeviceStatusObserver ","Observers after removed size is >>>>:"+observers.size());

    }

    public static void notifyStatusUpdate(final int objType, final int objId){

        for (int i = 0; i < observers.size(); i++) {
//            CCTWViewController cctwViewController = (CCTWViewController) observers.get(i);
//            if (cctwViewController.getRelativeId() == objId){
                observers.get(i).update( objType, objId);
//            }
        }
    }

    public static void removeAll() {
        observers.clear();
    }

    public interface Observer{
       void update(final int objType, final int objId);
    }
}
