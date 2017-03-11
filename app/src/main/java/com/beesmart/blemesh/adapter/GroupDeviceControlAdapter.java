package com.beesmart.blemesh.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.nxp.ble.meshlib.CCTWUtil;
import com.nxp.ble.meshlib.MeshService;
import com.nxp.utils.po.DeviceInfoPO;

import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.activities.MainActivity;
import com.beesmart.blemesh.callback.OnDeviceItemClickListener;
import com.beesmart.blemesh.customwidgets.CCTWViewHolder;
import com.beesmart.blemesh.utils.DeviceStatusObserver;

public class GroupDeviceControlAdapter extends RecyclerView.Adapter<CCTWViewHolder> {

    private CCTWUtil mCctwUtil;

    private Context context;

    private List<DeviceInfoPO> deviceInfoPOs;

    MeshService meshService ;
    private OnDeviceItemClickListener itemClickListener;
    public GroupDeviceControlAdapter(Context context, List<DeviceInfoPO> deviceInfoPOs,OnDeviceItemClickListener itemClickListener) {
        this.deviceInfoPOs = deviceInfoPOs;
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
        // TODO 加载数据
        final DeviceInfoPO deviceInfoPO = deviceInfoPOs.get(position);
        viewHolder.setTypeAndId(Constants.CONTROL_TYPE_DEVICE,deviceInfoPO.getNodeId());
        viewHolder.tv_deviceName.setText(deviceInfoPO.getDeviceName());
//        boolean isActive = meshService.isActiveDevice(deviceInfoPO.getNodeId());
//        viewHolder.aSwitch_onoff.setEnabled(isActive);
        viewHolder.controlType = CCTWViewHolder.CONTROL_IN_GROUP;
        viewHolder.iv_devicelogo.setImageResource(R.mipmap.ic_bulb);
        //Delete t
        viewHolder.iv_devicelogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });

        //注册状态监听
//        viewHolder.setUiUpdateListener(uiUpdateListener);
        DeviceStatusObserver.subscribe(viewHolder);
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onListFragmentInteraction(position);
                }
                return true;
            }
        });
    }
    public Object getItemAtPositon(int position){
        return deviceInfoPOs.get(position);
    }
    @Override
    public int getItemCount() {
        return deviceInfoPOs.size();
    }


}
