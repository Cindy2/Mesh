package com.beesmart.blemesh.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.bean.ScanResultDevice;

/**
 * Created by alphawong on 2016/3/29.
 */
public class ScanResultAdapter extends BaseAdapter{
    private LayoutInflater mInflater;

		private ArrayList<ScanResultDevice> mScanResults;

		public ScanResultAdapter(Context context) {
			mInflater = LayoutInflater.from(context);

			mScanResults = new ArrayList<ScanResultDevice>();
		}

		public void addDevice(ScanResultDevice result) {

			boolean isalready = false;
			for (ScanResultDevice scanResult : mScanResults) {
				if (scanResult.address.equals(result.address)) {
					scanResult.rssi = result.rssi;
					isalready = true;
					break;
				}
			}
			if (!isalready) {
				mScanResults.add(result);
			}
		}

		public void clear() {
			mScanResults.clear();
		}

		@Override
		public int getCount() {
			return mScanResults.size();
		}

		@Override
		public Object getItem(int i) {
			return mScanResults.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup parent) {

			ScanResultViewHolder viewHolder;
			if (view == null) {
				view = mInflater.inflate(R.layout.listview_provision, parent, false);
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

			ScanResultDevice result = mScanResults.get(i);
			viewHolder.deviceName.setText(result.name);
			viewHolder.deviceAddress.setText(result.address);
			viewHolder.rssi.setText("RSSI: " + result.rssi + "db");
			viewHolder.typeIcon.setBackgroundResource(R.drawable.ic_type_bluetooth);
			viewHolder.signal.setImageLevel(100 + result.rssi);

			return view;
		}


	static class ScanResultViewHolder {
		TextView deviceName;
		TextView deviceAddress;
		TextView rssi;
		ImageView typeIcon;
		ImageView signal;
	}

}

