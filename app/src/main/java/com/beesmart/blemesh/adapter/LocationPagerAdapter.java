package com.beesmart.blemesh.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.List;

import com.beesmart.blemesh.dao.po.LocationInfoPO;
import com.beesmart.blemesh.fragments.LocationDeviceControlFragment;

/**
 * Created by alphawong on 2016/3/12.
 */
public class LocationPagerAdapter extends FragmentPagerAdapter {

    private List<LocationInfoPO> locationInfoPOs;
    public LocationPagerAdapter(FragmentManager fm,List<LocationInfoPO> locationInfoPOs) {
        super(fm);
        this.locationInfoPOs = locationInfoPOs;
    }

    @Override
    public int getCount() {
        return locationInfoPOs.size();
    }


    @Override
    public CharSequence getPageTitle(int position) {
        return locationInfoPOs.get(position).getLocationName();
    }

    @Override
    public Fragment getItem(int position) {
        return LocationDeviceControlFragment.newInstance(locationInfoPOs.get(position).getLocationId());
    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        //super.destroyItem(container, position, object);

    }
}
