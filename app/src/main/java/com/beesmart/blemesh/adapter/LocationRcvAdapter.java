package com.beesmart.blemesh.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.callback.OnRecyclerViewItemClickListener;
import com.beesmart.blemesh.dao.po.LocationInfoPO;

/**
 * Created by alphawong on 2016/4/25.
 */
public class LocationRcvAdapter extends RecyclerView.Adapter<LocationRcvAdapter.LocationViewHolder>{
    private OnRecyclerViewItemClickListener itemClickListener;
    Context context;
    List<LocationInfoPO> locationInfoPOs;
    public int selectLocation = -1;
    int viewWidth;
    public LocationRcvAdapter(Context context, List<LocationInfoPO> locationInfoPOs){
        this.context = context;
        this.locationInfoPOs = locationInfoPOs;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =((Activity)context).getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.viewWidth = displayMetrics.widthPixels / 3;
    }

    public void setItemClickListener(OnRecyclerViewItemClickListener itemClickListener){
        this.itemClickListener = itemClickListener;
    }
    @Override
    public LocationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location_top,parent,false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final LocationViewHolder holder, final int position) {
        final LocationInfoPO locationInfoPO = locationInfoPOs.get(position);

        if (locationInfoPO.getLocationId() == selectLocation){
            holder.vFlag.setVisibility(View.VISIBLE);
        }else{
            holder.vFlag.setVisibility(View.GONE);
        }

        holder.tvLocation.setText(locationInfoPO.getLocationName());
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onItemClick(position);
                selectLocation = locationInfoPO.getLocationId();
                notifyDataSetChanged();
                EventBus.getDefault().post(locationInfoPO);
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return itemClickListener.onItemLongClick(position,holder.mView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locationInfoPOs.size();
    }

    public class LocationViewHolder extends RecyclerView.ViewHolder{
        private TextView tvLocation;
        private View vFlag;
        private View mView;
        public LocationViewHolder(View view){
            super(view);
            mView = view;
            tvLocation = (TextView) view.findViewById(R.id.tv_item_location);
            vFlag = view.findViewById(R.id.item_check_flag);

            ViewGroup.LayoutParams layoutParams = mView.getLayoutParams();
            layoutParams.width = viewWidth;
            mView.setLayoutParams(layoutParams);
        }
    }
}
