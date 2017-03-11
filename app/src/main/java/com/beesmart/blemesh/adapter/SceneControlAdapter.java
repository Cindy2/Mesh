package com.beesmart.blemesh.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.nxp.ble.meshlib.CCTWUtil;
import com.nxp.ble.meshlib.MeshService;

import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.customwidgets.CCTWViewHolder;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;

/**
 * Created by alphawong on 2016/4/2.
 */
public class SceneControlAdapter extends ArrayAdapter<LocationAffiliationPO>{

    private CCTWUtil mCctwUtil;

    public SceneControlAdapter(Context context, MeshService meshService, List<LocationAffiliationPO> objects) {
        super(context, -1, objects);
        mCctwUtil = new CCTWUtil(context,meshService);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        CCTWViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_control_cctw, parent, false);
        }
        viewHolder = new CCTWViewHolder(convertView, mCctwUtil, getContext());
        // 加载数据
        final LocationAffiliationPO locationAffiliationPO = getItem(position);
        viewHolder.setTypeAndId(locationAffiliationPO.getRelativeType(), locationAffiliationPO.getRelativeId());
        viewHolder.tv_deviceName.setText(locationAffiliationPO.getRelativeName());
        if (locationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE) {//设备
            viewHolder.iv_devicelogo.setImageResource(R.mipmap.ic_bulb);
            viewHolder.tv_deviceName.setTextColor(Color.parseColor("#919191"));
            //Delete t
        } else if (locationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_GROUP) {
            viewHolder.tv_deviceName.setTextColor(Color.parseColor("#3F51B5"));
            viewHolder.iv_devicelogo.setImageResource(R.mipmap.ic_bulb_group);
        }
        return convertView;
    }


}
