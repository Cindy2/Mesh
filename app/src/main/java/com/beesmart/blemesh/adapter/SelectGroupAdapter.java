package com.beesmart.blemesh.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.nxp.utils.po.GroupInfoPO;

import java.util.ArrayList;
import java.util.HashMap;

import com.beesmart.blemesh.R;

/**
 * Created by alphawong on 2016/3/7.
 */
public class SelectGroupAdapter extends BaseAdapter{
    private ArrayList<Integer> grouplist;
    private HashMap<Integer, Boolean> isSelected;
    private Context mContext;
    public SelectGroupAdapter(Context mContext) {
        this.mContext = mContext;
        grouplist = new ArrayList<>();
        isSelected = new HashMap<Integer, Boolean>();
    }

    public void addGroup(int groupId, boolean is) {
        grouplist.add(Integer.valueOf(groupId));
        isSelected.put(Integer.valueOf(groupId), is);
    }

    public int getGroup(int position) {
        return grouplist.get(position);
    }

    public boolean getStatus(int position) {
        return isSelected.get(getGroup(position));
    }

    public void setStatus(int position, boolean is) {
        isSelected.put(getGroup(position), is);
    }

    public int getSelectedNumber() {
        int count = 0;
        for (int i = 0; i < getCount(); i++) {
            if (getStatus(i)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getCount() {
        return grouplist.size();
    }

    @Override
    public Object getItem(int position) {
        return grouplist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        SelectGroupViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.view_selectgroup, parent, false);
            viewHolder = new SelectGroupViewHolder();
            viewHolder.groupName = (TextView) view.findViewById(R.id.textview_selectgroup);
            viewHolder.checkBox = (CheckBox) view.findViewById(R.id.checkbox_selectgroup);
            view.setTag(viewHolder);
        } else {
            viewHolder = (SelectGroupViewHolder) view.getTag();
        }

        viewHolder.groupName.setText(GroupInfoPO.getGroupInfoPO(getGroup(position)).getGroupName());
        viewHolder.checkBox.setChecked(getStatus(position));

        return view;
    }

    private static class SelectGroupViewHolder {
        TextView groupName;
        CheckBox checkBox;
    }
}
