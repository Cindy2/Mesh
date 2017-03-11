package com.beesmart.blemesh.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.dao.po.LocationInfoPO;

/**
 * Created by alphawong on 2016/4/18.
 */
public class LocationAdapter extends ArrayAdapter<LocationInfoPO> {
    public int selectLocationId = -1;
    public LocationAdapter(Context context,List<LocationInfoPO> objects) {
        super(context, -1, objects);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_location,parent,false);
            holder = new ViewHolder();
            holder.tvLocation = (TextView) convertView.findViewById(R.id.tv_item_location);
            holder.itemCheck = convertView.findViewById(R.id.item_check_flag);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvLocation.setText(getItem(position).getLocationName());
        if (getItem(position).getLocationId() == selectLocationId){
            holder.itemCheck.setVisibility(View.VISIBLE);
        }else{
            holder.itemCheck.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

    class ViewHolder {
        View itemCheck;
        TextView tvLocation;
    }
}
