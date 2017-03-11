package com.beesmart.blemesh.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.dao.po.SceneDeviceOperationPO;
import com.beesmart.blemesh.dao.po.SceneInfoPO;
import com.beesmart.blemesh.fragments.DeviceControlFragment.OnListFragmentInteractionListener;

public class SceneRecyclerViewAdapter extends RecyclerView.Adapter<SceneRecyclerViewAdapter.ViewHolder> {
    Context context;
    private  List<SceneInfoPO> sceneInfoPOs;
    private  OnListFragmentInteractionListener mListener;
    int viewHeight;

    int[] colors = new int[]{android.R.color.holo_blue_bright,android.R.color.holo_blue_light,android.R.color.holo_green_light
            ,android.R.color.holo_orange_light,android.R.color.holo_red_light,android.R.color.holo_purple,android.R.color.holo_green_dark
    };

    public SceneRecyclerViewAdapter(Context context,List<SceneInfoPO> sceneInfoPOs, OnListFragmentInteractionListener listener) {
        this.sceneInfoPOs = sceneInfoPOs;
        mListener = listener;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =((Activity)context).getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.viewHeight = displayMetrics.widthPixels / 2;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scene_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        Log.d("SceneRecyclerViewAdapte","recycled>>>>>:"+holder.toString());
        super.onViewRecycled(holder);
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = sceneInfoPOs.get(position);
        holder.mContentView.setText(sceneInfoPOs.get(position).getSceneName());
        holder.mContentView.setBackgroundColor(colors[((int)Math.random()*colors.length)]);//标签的背景色
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                final AlertDialog.Builder builder= new AlertDialog.Builder(context);
                builder.setItems(R.array.scene_operate_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
//                                Snackbar.make(holder.mView,"重命名",Snackbar.LENGTH_SHORT).show();
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(context);

                                View view = View.inflate(context,R.layout.dialog_addgroup,null);
                                final EditText etName = (EditText) view.findViewById(R.id.edittext_groupname);
                                etName.setText(holder.mItem.getSceneName());
                                builder1.setTitle(R.string.rename_scene).setView(view);
                                builder1.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String newSceneName = etName.getText().toString();
                                        if(!SceneInfoPO.isSceneNameExists(newSceneName)){

                                            holder.mItem.setSceneName(newSceneName);
                                            holder.mItem.save();
                                            notifyItemChanged(position);
                                        }else{
                                            Snackbar.make(etName,"修改失败，改名字已存在！",Snackbar.LENGTH_SHORT).show();
                                        }

                                    }
                                });
                                builder1.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });
                                builder1.create().show();
                                break;

                            case 1:
                                Snackbar.make(holder.mView,"编辑情景",Snackbar.LENGTH_SHORT).show();
                                //TODO
                                break;
                            case 2:
//                                Snackbar.make(holder.mView,"删除情景",Snackbar.LENGTH_SHORT).show();
//                                notifyItemRemoved(position);
                                holder.mItem.delete();
                                sceneInfoPOs.remove(sceneInfoPOs.get(position));
                                SceneDeviceOperationPO.deleteScene(holder.mItem.getSceneId());
                                notifyDataSetChanged();
                                break;
                        }
                    }
                });
                builder.create().show();
                return false;

            }
        });
    }

    @Override
    public int getItemCount() {
        return sceneInfoPOs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public View mView;
        public TextView mContentView;
        public SceneInfoPO mItem;
        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (TextView) view.findViewById(R.id.tv_item_scene_name);
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
