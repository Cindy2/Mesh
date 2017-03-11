package com.beesmart.blemesh.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import com.nxp.ble.meshlib.CCTWUtil;
import com.nxp.utils.po.GroupInfoPO;
import com.nxp.utils.po.GroupOperationPO;

import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.activities.MainActivity;
import com.beesmart.blemesh.customwidgets.CCTWViewController;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.utils.DeviceStatusObserver;

/**
 * Created by alphawong on 2016/3/14.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private ExpandableListView.OnGroupClickListener groupClickListener = null;
    private CCTWUtil mCctwUtil;

    private Context context;

    private List<LocationAffiliationPO> locationAffiliationPOs;

    private CCTWViewController.OnUIUpdateListener uiUpdateListener = new CCTWViewController.OnUIUpdateListener() {
        @Override
        public void onUpdate() {
            notifyDataSetChanged();
        }
    };
    public ExpandableListAdapter(Context context,List<LocationAffiliationPO> locationAffiliationPOs) {
        this.context = context;
        this.locationAffiliationPOs = locationAffiliationPOs;
        mCctwUtil = new CCTWUtil(context,((MainActivity)context).getMeshService());
    }
    public void setOnGroupClickLitener(ExpandableListView.OnGroupClickListener groupClickListener){
        this.groupClickListener = groupClickListener;
    }
    @Override
    public int getGroupCount() {
        return locationAffiliationPOs.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int type = locationAffiliationPOs.get(groupPosition).getRelativeType();
        int relativeId = locationAffiliationPOs.get(groupPosition).getRelativeId();


        if (type == Constants.CONTROL_TYPE_DEVICE){
            return 0;
        }else if (type == Constants.CONTROL_TYPE_GROUP){
            GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(relativeId);
            return groupInfoPO.getDeviceListAffiliations().size();

        }else
            return 0;
//        return 3;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return locationAffiliationPOs.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        CCTWViewController viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_control_cctw, parent, false);
        }
            viewHolder = new CCTWViewController(convertView,mCctwUtil,context);
        // TODO 加载数据
        final LocationAffiliationPO locationAffiliationPO = locationAffiliationPOs.get(groupPosition);
        viewHolder.setTypeAndId(locationAffiliationPO.getRelativeType(),locationAffiliationPO.getRelativeId());
        viewHolder.tv_deviceName.setText(locationAffiliationPO.getRelativeName());
        if(locationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE){//设备
            //Delete t
            viewHolder.iv_devicelogo.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    return false;
                }
            });
        }else{
            //组
            GroupOperationPO groupOperationPO = GroupOperationPO.getGroupOperationPO(locationAffiliationPO.getRelativeId());
            viewHolder.tv_deviceName.setText(locationAffiliationPO.getRelativeName());
            viewHolder.iv_devicelogo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (groupClickListener != null)
                        groupClickListener.onGroupClick(null, null, groupPosition, 0);
                }
            });
        }
        //注册状态监听
        viewHolder.setUiUpdateListener(uiUpdateListener);
        DeviceStatusObserver.subscribe(viewHolder);
        return convertView;
    }

    @Override
    public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        CCTWViewController viewHolder = null;
            convertView = LayoutInflater.from(context).inflate(R.layout.item_control_cctw_in_group,parent,false);
            viewHolder = new CCTWViewController(convertView,mCctwUtil,context);
        // TODO 加载数据
//        group
        return convertView;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


}
