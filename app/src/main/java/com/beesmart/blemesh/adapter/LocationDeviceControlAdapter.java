package com.beesmart.blemesh.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.nxp.ble.meshlib.CCTWUtil;
import com.nxp.ble.meshlib.MeshService;

import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.activities.MainActivity;
import com.beesmart.blemesh.callback.OnDeviceItemClickListener;
import com.beesmart.blemesh.customwidgets.CCTWViewHolder;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.fragments.GroupDeviceControlFragment;
import com.beesmart.blemesh.utils.DeviceStatusObserver;

public class LocationDeviceControlAdapter extends RecyclerView.Adapter<CCTWViewHolder> {

//    private int locationId ;
    private CCTWUtil mCctwUtil;

    private Context context;

    private List<LocationAffiliationPO> locationAffiliationPOs;

    MeshService meshService ;
    private OnDeviceItemClickListener itemClickListener;
    public LocationDeviceControlAdapter(Context context, List<LocationAffiliationPO> locationAffiliationPOs,
                                        OnDeviceItemClickListener itemClickListener) {
        this.locationAffiliationPOs = locationAffiliationPOs;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =((Activity)context).getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.context = context;
        meshService = ((MainActivity)context).getMeshService();
        mCctwUtil = new CCTWUtil(context,meshService);
        this.itemClickListener = itemClickListener;
    }

    /**
     * 由这个来判断生成不同设备类型的ViewHolder
     * @param position
     * @return
     */
    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public CCTWViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_control_cctw, parent, false);
        return new CCTWViewHolder(view,mCctwUtil,context);
    }

    @Override
    public void onViewRecycled(CCTWViewHolder holder) {
        Log.d("SceneRecyclerViewAdapte","recycled>>>>>:"+holder.toString());
        DeviceStatusObserver.unsubcribe(holder);
        super.onViewRecycled(holder);
    }


    @Override
    public void onBindViewHolder(final CCTWViewHolder viewHolder, final int position) {
        // 加载数据
        final LocationAffiliationPO locationAffiliationPO = locationAffiliationPOs.get(position);
        viewHolder.setTypeAndId(locationAffiliationPO.getRelativeType(),locationAffiliationPO.getRelativeId());
        viewHolder.tv_deviceName.setText(locationAffiliationPO.getRelativeName());
        viewHolder.controlType = CCTWViewHolder.CONTROL_IN_LOCATION;
        if(locationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE){//设备
//            boolean isActive = meshService.isActiveDevice(locationAffiliationPO.getRelativeId());
//            viewHolder.aSwitch_onoff.setEnabled(isActive);
            viewHolder.iv_devicelogo.setImageResource(R.mipmap.ic_bulb);
            viewHolder.tv_deviceName.setTextColor(Color.parseColor("#919191"));
            //Delete t
            viewHolder.iv_devicelogo.setOnClickListener(null);
        }else if(locationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_GROUP){
            viewHolder.aSwitch_onoff.setEnabled(true);
            viewHolder.tv_deviceName.setTextColor(Color.parseColor("#3F51B5"));
            viewHolder.iv_devicelogo.setImageResource(R.mipmap.ic_bulb_group);
            //组
//            GroupOperationPO groupOperationPO = GroupOperationPO.getGroupOperationPO(locationAffiliationPO.getRelativeId());
           //有一个按钮点击之后展开组控制，用Fragment
            viewHolder.iv_devicelogo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogFragment dialogFragment = new GroupDeviceControlFragment();

                    Bundle args = new Bundle();
                    args.putInt(GroupDeviceControlFragment.ARG_PARAM,locationAffiliationPO.getRelativeId());
                    dialogFragment.setArguments(args);
                    ((FragmentActivity)context).getSupportFragmentManager().beginTransaction().add(dialogFragment,"groupDeviceControl").commit();
                }
            });
        }
        //注册状态监听
//        viewHolder.setUiUpdateListener(uiUpdateListener);
        DeviceStatusObserver.subscribe(viewHolder);
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener!=null){

                    itemClickListener.onListFragmentInteraction(position);
                }
                return true;
            }
        });
    }
    public Object getItemAtPositon(int position){
        return locationAffiliationPOs.get(position);
    }
    @Override
    public int getItemCount() {
        return locationAffiliationPOs.size();
    }


}
