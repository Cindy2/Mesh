package com.beesmart.blemesh.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;

/**
 * Created by alphawong on 2016/3/26.
 */
public class SelectSceneDeviceAdapter extends ArrayAdapter<LocationAffiliationPO>  {
    Context context;

    public Map<Integer,Boolean> isItemSelect = new HashMap<>();

    List<LocationAffiliationPO> locationAffiliationPOs;

    public SelectSceneDeviceAdapter(Context context, List<LocationAffiliationPO> objects) {
        super(context, -1, objects);
        locationAffiliationPOs = objects;
        this.context = context;
        for (int i = 0; i < locationAffiliationPOs.size(); i++) {
            isItemSelect.put(i,false);
        }
    }
//

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_select_device,parent,false);
//            convertView = super.getView(position, convertView, parent);;
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        LocationAffiliationPO info = locationAffiliationPOs.get(position);
        if (info.getRelativeType() == Constants.CONTROL_TYPE_GROUP){
            holder.iv_deviceType.setImageResource(R.mipmap.ic_bulb_group);
        }else{
            holder.iv_deviceType.setImageResource(R.mipmap.ic_bulb);
        }
        holder.tv_device_name.setText(info.getRelativeName());
        //判断设备类型，显示不同图标

        holder.tv_device_name.setChecked(isItemSelect.get(position));
        holder.tv_location.setChecked(isItemSelect.get(position));
        String sec  = getSectionForPosition(position);
        if (position == getPositionForSection(sec)) {
            String locationName = info.getLocationName();
            holder.tv_location.setVisibility(View.VISIBLE);
            holder.tv_location.setText(locationName);
            holder.tv_location.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckedTextView) v).isChecked();
//                    if (checked){
                       for (int i = position;i< locationAffiliationPOs.size();i++){
                           if (locationAffiliationPOs.get(i).getLocationId() == getItem(position).getLocationId()){
                               isItemSelect.put(i,!checked);
                           }
//                       }
                        notifyDataSetChanged();
                    }
                }
            });
        }else{
            holder.tv_location.setVisibility(View.GONE);
        }

        return convertView;
    }


    public int getPositionForSection(String sectionStr) {
        for (int i = 0; i < getCount(); i++) {
            String sortStr = getItem(i).getLocationName();
            if (sortStr!=null&&sortStr.equals(sectionStr)) {
                return i;
            }
        }

        return -1;
    }

    public String getSectionForPosition(int position) {
        return getItem(position).getLocationName();
    }

    private class ViewHolder{
        ImageView iv_deviceType;
        CheckedTextView tv_device_name;
        CheckedTextView tv_location;
        public ViewHolder(View convertView){
            iv_deviceType = (ImageView) convertView.findViewById(R.id.iv_item_device_type);
            tv_location = (CheckedTextView) convertView.findViewById(R.id.tv_item_location);
            tv_device_name = (CheckedTextView) convertView.findViewById(R.id.tv_item_device_name);
        }
    }
}
