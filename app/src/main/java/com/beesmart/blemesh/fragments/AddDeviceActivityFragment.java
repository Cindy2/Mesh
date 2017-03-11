package com.beesmart.blemesh.fragments;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beesmart.blemesh.R;

/**
 *
 */
public class AddDeviceActivityFragment extends Fragment {

    public AddDeviceActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView( inflater,container,savedInstanceState);
        return inflater.inflate(R.layout.fragment_add_device, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View view = getView();

    }
}
