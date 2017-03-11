package com.beesmart.blemesh.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.adapter.LocationRcvAdapter;
import com.beesmart.blemesh.callback.OnRecyclerViewItemClickListener;
import com.beesmart.blemesh.customwidgets.RecycleViewDivider;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.LocationInfoPO;
import com.beesmart.blemesh.utils.PreferenceUtils;

public class MainControlFragment extends Fragment {
//    ViewPager viewPager ;
//    TabLayout tabLayout;
    private Bundle arguments;
//    LocationPagerAdapter locationPagerAdapter;
    List<LocationInfoPO> locationInfoPos;
    private RecyclerView rcvLocation;
    LinearLayoutManager layoutManager;

    private LocationInfoPO mLocationInfoPO;

    private int mLocationId = -1;

    private static final String LAST_SELECT_LOCATION = "last_select_location";

    private Dialog mDialog;

    LocationRcvAdapter locationRcvAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            arguments = getArguments();
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        PreferenceUtils.setPrefInt(getActivity(),LAST_SELECT_LOCATION,mLocationId);
    }

    @Subscribe
    public void onLocationUpdate(LocationAffiliationPO param){
        locationInfoPos.clear();
        locationInfoPos.addAll(LocationInfoPO.getLocationInfoPos(true));
        locationRcvAdapter.notifyDataSetChanged();
    }
    @Subscribe
    public void onLocationSelected(LocationInfoPO param){
        Log.e("onLocationSelected","locationId:"+param.getLocationId()+"::"+param.getLocationName());
        mLocationId = param.getLocationId();
        getChildFragmentManager().beginTransaction().replace(R.id.fl_device_content,LocationDeviceControlFragment.newInstance(mLocationId)).commit();
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        viewPager = (ViewPager) getView().findViewById(R.id.vp_location_devices);
//        tabLayout = (TabLayout) getView().findViewById(R.id.tab_main_locations);

        locationInfoPos = LocationInfoPO.getLocationInfoPos(true);

        rcvLocation = (RecyclerView) getView().findViewById(R.id.rcv_location);
        layoutManager = new LinearLayoutManager(getActivity(),LinearLayoutManager.HORIZONTAL,false);
        rcvLocation.setLayoutManager(layoutManager);
        rcvLocation.addItemDecoration(new RecycleViewDivider(getActivity(),LinearLayoutManager.VERTICAL));
        locationRcvAdapter = new LocationRcvAdapter(getActivity(), locationInfoPos);
        int lastLocation = PreferenceUtils.getPrefInt(getActivity(), LAST_SELECT_LOCATION, -1);
        if (lastLocation!=-1){
            locationRcvAdapter.selectLocation = lastLocation;
            getChildFragmentManager().beginTransaction().replace(R.id.fl_device_content,LocationDeviceControlFragment.newInstance(lastLocation)).commit();
        }
        rcvLocation.setAdapter(locationRcvAdapter);

        locationRcvAdapter.setItemClickListener(new OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(position>0) {
//                    layoutManager.scrollToPosition(position - 1);
                    layoutManager.smoothScrollToPosition(rcvLocation,null,position-1);
                }

            }

            @Override
            public boolean onItemLongClick(int position,View view) {

                mLocationInfoPO = locationInfoPos.get(position);
                if (mLocationInfoPO.getLocationId() == LocationInfoPO.DEFAULT_LOCATION){
                    return false;
                }
                new AlertDialog.Builder(getActivity()).setItems(R.array.device_operate_menu,null);
                PopupMenu popupMenu = new PopupMenu(getActivity(), view);
                popupMenu.getMenuInflater().inflate(R.menu.menu_location,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()){
                            case R.id.action_location_rename:

                                createRenameLocationDialog();

                                break;
                            case R.id.action_location_add:
                                createAddNewLocationDialog();
                                break;
                            case R.id.action_location_delete:
                                createDeleteLocationDialog();
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
                return true;
            }
        });
        //查询地点列表

//        locationPagerAdapter = new LocationPagerAdapter(getChildFragmentManager(), locationInfoPos);
//        viewPager.setAdapter(locationPagerAdapter);
//
//
//
//        tabLayout.setupWithViewPager(viewPager);
//        if (locationInfoPos.size() <= 4){
//            tabLayout.setTabMode(TabLayout.MODE_FIXED);
//        }else {
//            tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
//        }
//        registerForContextMenu(tabLayout);



    }

    private void createDeleteLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.delete_location);
        builder.setMessage("Are you sure to delete "+mLocationInfoPO.getLocationName()+"?");
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mLocationInfoPO.delete();
                locationInfoPos.remove(mLocationInfoPO);
                if (mLocationId == mLocationInfoPO.getLocationId()){
                    mLocationId = 0;
                    locationRcvAdapter.selectLocation = mLocationId;
                    getChildFragmentManager().beginTransaction().replace(R.id.fl_device_content,LocationDeviceControlFragment.newInstance(mLocationId)).commit();
                }
                locationRcvAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(R.string.Cancel,null);
        if (mDialog !=null) mDialog.dismiss();
        mDialog = builder.create();
        mDialog.show();
    }

    /**增加新的location*/
    private void createRenameLocationDialog() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.rename_location);
        builder.setCancelable(false);

        final LinearLayout addgroupLayout = (LinearLayout)LayoutInflater.from(getActivity()).inflate(R.layout.dialog_addgroup, null,
                false);
        builder.setView(addgroupLayout);

        final EditText editText = (EditText) addgroupLayout.findViewById(R.id.edittext_groupname);
        editText.setText(mLocationInfoPO.getLocationName());
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                boolean locationNameExists = LocationInfoPO.isLocationNameExists(editText.getText().toString());
                if (!locationNameExists){
                    mLocationInfoPO.setLocationName(editText.getText().toString());
                    mLocationInfoPO.save();
                    locationRcvAdapter.notifyDataSetChanged();
                }else{
                    Toast.makeText(getActivity(), editText.getText().toString()+"is exists!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_control, container, false);
    }
    /**
     * Add new Lcation
     * 新增设备位置
     */
    private void createAddNewLocationDialog(){
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.add_new_Location);
        builder.setMessage(R.string.enter_location_name);
        builder.setCancelable(false);

        final LinearLayout addgroupLayout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.dialog_addgroup, null,
                false);
        builder.setView(addgroupLayout);

        final EditText editText = (EditText) addgroupLayout.findViewById(R.id.edittext_groupname);

//        builder.set
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        if (mDialog!=null){
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();

        Button positiveButton = ((android.app.AlertDialog)mDialog).getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //保存名称
                String name = editText.getText().toString();
                if (!name.isEmpty()) {
                    boolean isLocationNameExists = LocationInfoPO.isLocationNameExists(name);
                    if (!isLocationNameExists){
                        LocationInfoPO locationInfoPO = LocationInfoPO.createLocationPO();
                        locationInfoPO.setLocationName(name);
                        locationInfoPO.setNodeId(System.currentTimeMillis());//use system time as nodeId
                        locationInfoPO.save();
                        locationInfoPos.add(locationInfoPO);
                        locationRcvAdapter.notifyDataSetChanged();
                        mDialog.dismiss();
                        Toast.makeText(getActivity(), R.string.add_new_Location_successful, Toast.LENGTH_SHORT).show();
                    }else{
                        Snackbar.make(editText,R.string.this_location_name_is_exists,Snackbar.LENGTH_SHORT).show();
                    }
                }else{
                    Snackbar.make(editText,R.string.location_name_cantnot_be_empty,Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

}
