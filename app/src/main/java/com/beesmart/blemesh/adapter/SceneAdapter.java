package com.beesmart.blemesh.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.dao.po.SceneInfoPO;

public class SceneAdapter extends ArrayAdapter<SceneInfoPO> {
    Context context;
    int viewHeight;
    public int selection = -1;
    int[] colors = new int[]{/*android.R.color.holo_blue_bright,*/android.R.color.holo_blue_light,android.R.color.holo_green_light
            ,android.R.color.holo_orange_light,android.R.color.holo_red_light,android.R.color.holo_purple,android.R.color.holo_green_dark
    };

    public SceneAdapter(Context context, List<SceneInfoPO> sceneInfoPOs) {
        super(context,-1,sceneInfoPOs);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =((Activity)context).getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.viewHeight = displayMetrics.widthPixels / 2;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_scene_list,parent,false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        final SceneInfoPO item = getItem(position);
        holder.mItem = getItem(position);
        holder.mContentView.setText(item.getSceneName());

        holder.mSceneFlag.setVisibility(selection==item.getSceneId()?View.VISIBLE:View.GONE);
        holder.mContentView.setBackgroundResource(colors[position%colors.length]);//标签的背景色
        return convertView;
    }

    public class ViewHolder {
        public View mView;
        public TextView mContentView;
        public SceneInfoPO mItem;
        public View mSceneFlag;
        public ViewHolder(View view) {
            mView = view;
            mContentView = (TextView) view.findViewById(R.id.tv_item_scene_name);
            mSceneFlag = view.findViewById(R.id.tv_scene_apply_flag);
            ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();

            layoutParams.height = viewHeight;
            mView.setLayoutParams(layoutParams);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }

    }
}
