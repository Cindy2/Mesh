package com.beesmart.blemesh.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nxp.ble.meshlib.BLEDevice;

import java.util.List;

import com.beesmart.blemesh.DeviceType;
import com.beesmart.blemesh.R;

/**
 * Created by alphawong on 2016/3/1.
 * 扫描蓝牙设备的listview Adapter
 */
public class ScanDeviceAdapter extends ArrayAdapter<BLEDevice> {

    public ScanDeviceAdapter(Context context, List<BLEDevice> objects) {
        super(context, -1, objects);
    }
    public void addDevice(BLEDevice result) {

        boolean already = false;
//        for (BLEDevice deviceInfo : ) {
//            if (deviceInfo.getMacAddress().equals(result.getMacAddress())) {
//                deviceInfo.setRSSI(result.getRSSI());
//                already = true;
//                break;
//            }
//        }

        for (int i = 0;i<getCount();i++){
            BLEDevice deviceInfo = getItem(i);
            if (deviceInfo.getMacAddress().equals(result.getMacAddress())) {
                deviceInfo.setRSSI(result.getRSSI());
                already = true;
                break;
            }
        }

        if (!already) {
            this.add(result);
        }
    }

    public void remove(int index){
        remove(getItem(index));
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {

        ScanResultViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.listview_provision, parent, false);
            viewHolder = new ScanResultViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.text_device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.text_device_name);
            viewHolder.rssi = (TextView) view.findViewById(R.id.text_rssi);
            viewHolder.typeIcon = (ImageView) view.findViewById(R.id.icon_device_type);
            viewHolder.signal = (ImageView) view.findViewById(R.id.image_rssi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ScanResultViewHolder) view.getTag();
        }

        BLEDevice result = getItem(i);
        viewHolder.deviceName.setText(result.getDeviceName());
        viewHolder.deviceAddress.setText(result.getMacAddress());
        viewHolder.rssi.setText("RSSI: " + result.getRSSI() + "db");
        viewHolder.typeIcon.setBackgroundResource(DeviceType.getType(result.getDeviceType()).getDrawable());
        viewHolder.signal.setImageLevel(100 + result.getRSSI());

        return view;
    }
    private class ScanResultViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView rssi;
        ImageView typeIcon;
        ImageView signal;
    }
}
