package com.beesmart.blemesh.callback;

import android.view.View;

/**
 * Created by alphawong on 2016/4/25.
 */
public interface OnRecyclerViewItemClickListener {
    public void onItemClick(int position);
    public boolean onItemLongClick(int position, View view);
}
