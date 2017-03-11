package com.beesmart.blemesh.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.ble.meshlib.BLEDevice;
import com.nxp.ble.meshlib.MeshService;
import com.nxp.ble.meshlib.callback.IProvisionCallback;
import com.nxp.ble.meshlib.callback.IStartScanCallback;
import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.GroupInfoPO;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.adapter.ScanDeviceAdapter;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.LocationInfoPO;
import com.beesmart.blemesh.utils.AppManager;
import com.beesmart.blemesh.utils.PreferenceConstants;
import com.beesmart.blemesh.utils.PreferenceUtils;

/**
 * Add devices to Mesh Network
 * @author AlphaWong
 */
public class ProvisionDeviceActivity extends AppCompatActivity {
    public MeshService mMeshService;
    
    private ScanDeviceAdapter mScanResultAdapter;

    private View mView;

    private ProgressDialog mProgressDialog;

    private boolean mScanning = false;

    private int mScanResultChoice = 0;
    private BLEDevice mProvisionDevice;

    private int mSwitchControl = 0;
    private int mSwitchControlDeiceIndex = 0;
    private int mSwitchControlGroupIndex = 0;

    private Dialog mDialog;

    ListView lvDevices;

    SwipeRefreshLayout swipeRefreshLayout ;

    private LocationInfoPO mLocationInfoPO;
    //上一个设备选择的位置
    private int mLastSelectLocationIndex = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AppManager.getAppManager().addActivity(this);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //完成设备添加
                stopScanDevice();
                PreferenceUtils.setPrefBoolean(ProvisionDeviceActivity.this, PreferenceConstants.FIRST_USE,false);
                startActivity(new Intent(ProvisionDeviceActivity.this, MainActivity.class));
                EventBus.getDefault().post(new LocationAffiliationPO());
                AppManager.getAppManager().finishAllActivity();
            }
        });
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_refresh);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light,
                android.R.color.holo_orange_light, android.R.color.holo_green_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startScanDevice();
            }
        });

        lvDevices = (ListView) findViewById(R.id.lv_devices);
        lvDevices.setOnItemClickListener(mDeviceitemClickListener);
        mScanResultAdapter = new ScanDeviceAdapter(this,new ArrayList<BLEDevice>());
        lvDevices.setAdapter(mScanResultAdapter);

        LocationInfoPO.createDefaultLoation(getString(R.string.label_ALL));
    }

    AdapterView.OnItemClickListener mDeviceitemClickListener = new AdapterView.OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mScanResultChoice = position;
            mProvisionDevice = mScanResultAdapter.getItem(position);
            if (mScanning){
                stopScanDevice();
//                return ;
            }

            //设置Key
            String key = PreferenceUtils.getPrefString(ProvisionDeviceActivity.this, Constants.KEY,"");
            mProvisionDevice.setSetupKey(key);
            //命名设备
            createSetProvisionNameDialog();
        }
    } ;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_device, menu);
        if (mScanning) {
            menu.findItem(R.id.action_stop).setVisible(true);
            menu.findItem(R.id.action_scan).setVisible(false);
        } else {
            menu.findItem(R.id.action_stop).setVisible(false);
            menu.findItem(R.id.action_scan).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         super.onOptionsItemSelected(item);
        switch(item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_scan:
                swipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(true);
                        startScanDevice();
                    }
                });
                invalidateOptionsMenu();
                break;

            case R.id.action_stop:

                CSLog.i(ProvisionDeviceActivity.class, "stopScanning");
                stopScanDevice();
                break;
        }

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        //绑定MeshService
        if (mMeshService == null) {
            CSLog.i(MainActivity.class, "start bind mesh service");
            final Intent meshServiceIntent = new Intent(this, MeshService.class);
            bindService(meshServiceIntent, mMeshServiceConnection, Context.BIND_AUTO_CREATE);
        }

    }


    protected void onDestroy() {
        super.onDestroy();

        unbindMeshService();
    }

    private void stopScanDevice(){
        if (getMeshService() != null) {
            getMeshService().stopScanning();
        }
        swipeRefreshLayout.setRefreshing(false);
        mScanning = false;
        invalidateOptionsMenu();
    }

    /**
     * 开始搜索设备
     */
    private void startScanDevice(){
        mScanResultAdapter.clear();
        mScanning = true;
        if (getMeshService() != null) {
            getMeshService().startScanning(Constants.SCAN_PERIOD, mStartScanCallback);
        }else{
//            Toast.makeText(this,"MeshService is not available,please try again later！",Toast.LENGTH_SHORT).show();
            Snackbar.make(swipeRefreshLayout,"MeshService is not available,please try again later！",Snackbar.LENGTH_SHORT).show();
        }
        invalidateOptionsMenu();
    }

    /**
     * BLEMesh Service 绑定Activity回调
     */
    public final ServiceConnection mMeshServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            CSLog.i(MainActivity.class, "Mesh service onServiceConnected ");
            mMeshService = ((MeshService.LocalBinder) service).getService();
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                    startScanDevice();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            CSLog.i(MainActivity.class, "Mesh service onServiceDisconnected ");
            mMeshService = null;
        }
    };

    public MeshService getMeshService() {
        return mMeshService;
    }

    public void unbindMeshService() {
        if (mMeshService != null) {
            CSLog.i(MainActivity.class, "unbind mesh service");
            unbindService(mMeshServiceConnection);
            mMeshService = null;
        }
    }

    /**
     * 开始搜索蓝牙设备回调
     */
    private IStartScanCallback mStartScanCallback = new IStartScanCallback() {

        @Override
        public void onTimeout() {
            CSLog.i(ProvisionDeviceActivity.class, "IStartScanCallback onTimeout");
            mScanning = false;
            invalidateOptionsMenu();
            swipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onScanResult(BLEDevice result) {
            CSLog.i(ProvisionDeviceActivity.class, "IStartScanCallback onScanResult: " + result.getMacAddress());

            mScanResultAdapter.addDevice(result);
            mScanResultAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            mScanning = false;
            CSLog.i(ProvisionDeviceActivity.class, "IStartScanCallback onScanFailed " + errorCode);

            swipeRefreshLayout.setRefreshing(false);
        }
    };

    /**
     * 设置蓝牙设备成为Mesh模式回调接口
     */
    private IProvisionCallback mIProvisionCallback = new IProvisionCallback() {

        @Override
        public void onTimeout() {
            CSLog.i(ProvisionDeviceActivity.class, "IProvisionCallback onTimeout");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.dismiss();

                    Toast.makeText(ProvisionDeviceActivity.this, R.string.setup_timeout, Toast.LENGTH_SHORT).show();
                    //TODO 重新设置一次
                    createReconnectDialog();
                }
            });

        }

        @Override
        public void onSuccess() {
            CSLog.i(ProvisionDeviceActivity.class, "IProvisionCallback onSuccess");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.dismiss();

                    if (mProvisionDevice.getDeviceName().contains("device")) {

                        int devicecount = PreferenceUtils.getPrefInt(ProvisionDeviceActivity.this,Constants.DEVICE_NAME_COUNT,0);
                        devicecount++;
                        PreferenceUtils.setPrefInt(ProvisionDeviceActivity.this,Constants.DEVICE_NAME_COUNT,devicecount);

                    } else if (mProvisionDevice.getDeviceName().contains("switch")) {

                        int switchcount = PreferenceUtils.getPrefInt(ProvisionDeviceActivity.this,Constants.SWITCH_NAME_COUNT,0);
                        switchcount++;
                        PreferenceUtils.setPrefInt(ProvisionDeviceActivity.this,Constants.SWITCH_NAME_COUNT,switchcount);
                    }


                    // save group type
                    if (mProvisionDevice.getDeviceType() != MeshConstants.DEVICE_TYPE_SWITCH) {
                        for (int groupId : mProvisionDevice.getGroupsId()) {
                            GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(groupId);
                            groupInfoPO.setGroupType(mProvisionDevice.getDeviceType());
                            groupInfoPO.save();
                        }
                    }
                    mScanResultAdapter.remove(mScanResultChoice);
//                    mScanResultAdapter.remove(mProvisionDevice);
                    mScanResultAdapter.notifyDataSetChanged();

                    Toast.makeText(ProvisionDeviceActivity.this, R.string.setup_successful, Toast.LENGTH_SHORT).show();

                    //TODO 保存设备与地点的关系
                    LocationAffiliationPO locationAffiliationPO = new LocationAffiliationPO();
                    locationAffiliationPO.setLocationId(mLocationInfoPO.getLocationId());
                    locationAffiliationPO.setLocationName(mLocationInfoPO.getLocationName());
                    int deviceId = DeviceInfoPO.getDeviceInfoPO(mProvisionDevice.getMacAddress()).getNodeId();
                    locationAffiliationPO.setRelativeId(deviceId);
                    locationAffiliationPO.setRelativeType(Constants.CONTROL_TYPE_DEVICE);
                    locationAffiliationPO.setRelativeName(mProvisionDevice.getDeviceName());
                    locationAffiliationPO.save();

                    PreferenceUtils.setPrefInt(ProvisionDeviceActivity.this,Constants.NETWORK_FLAG,1);
                }
            });
        }

        @Override
        public void onFailed(int errorCode) {
            CSLog.i(ProvisionDeviceActivity.class, "IProvisionCallback onFailed " + errorCode);
            Log.e("IProvisionCallback","IProvisionCallback onFailed " + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.dismiss();

                    Toast.makeText(ProvisionDeviceActivity.this, R.string.setup_failed, Toast.LENGTH_SHORT).show();
                createReconnectDialog();
                }
            });
        }

    };

    /**
     * 命名设备
     */
    private void createSetProvisionNameDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setup_name);
        builder.setMessage(R.string.enter_device_name);
        builder.setCancelable(false);

        final LinearLayout provisionnameLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_provisionname, null);
        builder.setView(provisionnameLayout);

        if (mProvisionDevice.getDeviceType() == MeshConstants.DEVICE_TYPE_SWITCH) {

            int switchcount = PreferenceUtils.getPrefInt(this, Constants.SWITCH_NAME_COUNT, 0);

            switchcount++;

            EditText nameEditText = (EditText) provisionnameLayout.findViewById(R.id.edittext_provision_name);
            nameEditText.setHint("switch"+ switchcount );
        } else {
            int devicecount = PreferenceUtils.getPrefInt(this, Constants.DEVICE_NAME_COUNT, 0);
            devicecount++;

            EditText nameEditText = (EditText) provisionnameLayout.findViewById(R.id.edittext_provision_name);
            nameEditText.setHint("device" + devicecount);
        }

        builder.setNegativeButton(R.string.Cancel, null);

        builder.setPositiveButton(R.string.OK, new ProvisionNameDialogListener(provisionnameLayout));

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();

    }

    /**
     * 设置设备名称
     */
    private class ProvisionNameDialogListener implements  DialogInterface.OnClickListener{
        View contentLayout;
        public ProvisionNameDialogListener(View provisionnameLayout) {
            this.contentLayout = provisionnameLayout;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            EditText editText = (EditText) contentLayout.findViewById(R.id.edittext_provision_name);
            String name = editText.getText().toString();

            //如果没有输入名称，则默认命名为自增数字
            if (name.equals(Constants.EMPTY)) {
                mProvisionDevice.setDeviceName(editText.getHint().toString());
            } else {
                mProvisionDevice.setDeviceName(name);
            }

            dialog.dismiss();

            if (mProvisionDevice.getDeviceType() == MeshConstants.DEVICE_TYPE_SWITCH) {

                createSwitchSelectDialog();

            } else if (mProvisionDevice.getDeviceType() > MeshConstants.DEVICE_TYPE_SWITCH) {

                // sensor
                createProvisionFinishDialog();

            } else {

//                createSetProvisionGroupDialog();
                createSetProvisionLocationDialog();
            }
        }
    }
//

    /**
     * 设置设备的所在位置，Location
     */
    private void createSetProvisionLocationDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //
        builder.setTitle(R.string.set_device_location);
        builder.setCancelable(false);
        builder.setNeutralButton(R.string.add_new_Location, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createAddNewLocationDialog();
            }
        });
        final List<LocationInfoPO> locationInfoPos = LocationInfoPO.getLocationInfoPos(false);
        String[] locationNames = new String[locationInfoPos.size()];
        for (int i= 0;i<locationInfoPos.size();i++){
            locationNames[i] = locationInfoPos.get(i).getLocationName();
        }
        //位置和组，设备的关系描述
        final LocationAffiliationPO locationAffiliationPO = new LocationAffiliationPO();
        if(locationInfoPos == null||locationInfoPos.size()==0){
            builder.setMessage(R.string.no_available_location);
        }else {
            if (mLastSelectLocationIndex != LocationAffiliationPO.INVALID_LOCATION_ID){
                mLocationInfoPO = locationInfoPos.get(mLastSelectLocationIndex);
                locationAffiliationPO.setLocationId(mLocationInfoPO.getId().intValue());
            }
            builder.setSingleChoiceItems(new ArrayAdapter<String>(ProvisionDeviceActivity.this,
                            android.R.layout.simple_list_item_multiple_choice, locationNames), mLastSelectLocationIndex,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //关联地点和设备
                            mLocationInfoPO = locationInfoPos.get(which);

                            locationAffiliationPO.setLocationId(mLocationInfoPO.getLocationId());
                            mLastSelectLocationIndex = which;

                        }
                    });
        }
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setNegativeButton(R.string.Cancel, null);
        if (mDialog != null){
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
        ((AlertDialog)mDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationAffiliationPO.getLocationId() == LocationAffiliationPO.INVALID_LOCATION_ID) {
                    Snackbar.make(v,R.string.plz_select_location, Snackbar.LENGTH_SHORT).show();
                }else {
                    createSetProvisionGroupDialog();
                    //不用选择分组了，直接显示设置完成，默认所在组id是1，即 全部
//                    ArrayList<Integer> defalutGroup = new ArrayList<>();
//                    defalutGroup.add(1);
                    List<Integer> selectedGroups = new ArrayList<Integer>() ;
                    mProvisionDevice.setGroupsId(selectedGroups);
                    createProvisionFinishDialog();
//                    final List<GroupInfoPO> groupInfoPOs = GroupInfoPO.getGroupList();
//                    final List<Integer> selectedGroups = new ArrayList<Integer>() ;
//                    mProvisionDevice.setGroupsId(selectedGroups);
//
//                    createProvisionFinishDialog();
                }
            }
        });

    }


    /**
     * Add new Lcation
     * 新增设备位置
     */
    private void createAddNewLocationDialog(){
        //TODO
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_new_Location);
        builder.setMessage(R.string.enter_location_name);
        builder.setCancelable(false);

        final LinearLayout addgroupLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_addgroup, null,
                false);
        builder.setView(addgroupLayout);

        final EditText editText = (EditText) addgroupLayout.findViewById(R.id.edittext_groupname);

//        builder.set
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createSetProvisionLocationDialog();
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

        Button positiveButton = ((AlertDialog)mDialog).getButton(AlertDialog.BUTTON_POSITIVE);
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
                        createSetProvisionLocationDialog();
                    }else{
                        Snackbar.make(editText,R.string.this_location_name_is_exists,Snackbar.LENGTH_SHORT).show();
                    }
                }else{
                    Snackbar.make(editText,R.string.location_name_cantnot_be_empty,Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void createSetProvisionGroupDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setup_groups);
        builder.setCancelable(false);


        builder.setNeutralButton(R.string.add_new_group, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createAddGroupDialog();
            }
        });

//
        final List<GroupInfoPO> groupInfoPOs = GroupInfoPO.getGroupList();
//        for (GroupInfoPO groupInfoPO : groupInfoPOs) {
//            if (groupInfoPO.getGroupType() == 0 || groupInfoPO.getGroupType() == mProvisionDevice.getDeviceType()) {
//                selectGroupAdapter.addGroup(groupInfoPO.getGroupId(), false);
//            }
//        }
        final List<Integer> selectedGroups = new ArrayList<Integer>() ;
        String[] groupNames = new String[groupInfoPOs.size()];
        for (int i = 0;i<groupInfoPOs.size();i++) {
               groupNames[i] = groupInfoPOs.get(i).getGroupName();
        }
        builder.setSingleChoiceItems(new ArrayAdapter<String>(ProvisionDeviceActivity.this, android.R.layout.simple_list_item_multiple_choice, groupNames),
                -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedGroups.clear();
                selectedGroups.add(groupInfoPOs.get(which).getGroupId());
            }
        });


        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
//                List<Integer> groups = new ArrayList<Integer>();
//                for (int i = 0; i < selectGroupAdapter.getCount(); i++) {
//                    if (selectGroupAdapter.getStatus(i)) {
//                        groups.add(selectGroupAdapter.getGroup(i));
//                    }
//                }

                mProvisionDevice.setGroupsId(selectedGroups);

                createProvisionFinishDialog();
            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                //不分组，默认到未分组里
                final List<GroupInfoPO> groupInfoPOs = GroupInfoPO.getGroupList();
                final List<Integer> selectedGroups = new ArrayList<Integer>() ;
                mProvisionDevice.setGroupsId(selectedGroups);

                createProvisionFinishDialog();
            }
        });

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
    }

    private void createAddGroupDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_group);
        builder.setCancelable(false);

        final LinearLayout addgroupLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_addgroup, null,
                false);
        builder.setView(addgroupLayout);

        EditText editText = (EditText) addgroupLayout.findViewById(R.id.edittext_groupname);

//        SharedPreferences sharedPreferences = getSharedPreferences(Constants.MY_DATA,
//                Activity.MODE_PRIVATE);
//        int groupcount = sharedPreferences.getInt(Constants.GROUP_NAME_COUNT, 0);
        int groupcount = PreferenceUtils.getPrefInt(this, Constants.GROUP_NAME_COUNT, 0);
        groupcount++;

        editText.setHint("group" + groupcount);

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int arg1) {

            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                createSetProvisionGroupDialog();
            }
        });

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();

        Button positiveButton = ((AlertDialog) mDialog).getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new AddGroupDialogListener(mDialog, addgroupLayout));
    }

    private class AddGroupDialogListener implements View.OnClickListener {
        private final Dialog dialog;
        private LinearLayout addgroupLayout;

        public AddGroupDialogListener(Dialog dialog, LinearLayout addgroupLayout) {
            this.dialog = dialog;
            this.addgroupLayout = addgroupLayout;
        }

        @Override
        public void onClick(View v) {

            EditText editText = (EditText) addgroupLayout.findViewById(R.id.edittext_groupname);
            String groupname = editText.getText().toString();
            if (groupname.equals(Constants.EMPTY)) {
                groupname = editText.getHint().toString();
            }

            boolean alreadyinGroup = GroupInfoPO.isGroupNameExist(groupname);

            if (groupname.equals(Constants.EMPTY)) {
                Toast.makeText(getApplicationContext(), R.string.group_name_not_empty, Toast.LENGTH_SHORT).show();
            } else if (alreadyinGroup) {
                Toast.makeText(getApplicationContext(), R.string.group_name_exits, Toast.LENGTH_SHORT).show();
            } else if (groupname.equals(getApplicationContext().getResources().getString(R.string.label_ALL))
                    || groupname.equals(getApplicationContext().getResources().getString(R.string.label_sensor))) {
                Toast.makeText(getApplicationContext(), R.string.group_name_exits, Toast.LENGTH_SHORT).show();
            } else {

                if (groupname.contains("group")) {
                    int groupcount = PreferenceUtils.getPrefInt(ProvisionDeviceActivity.this, Constants.GROUP_NAME_COUNT, 0);
                    groupcount++;
                    PreferenceUtils.setPrefInt(ProvisionDeviceActivity.this, Constants.GROUP_NAME_COUNT,groupcount);
                }

                GroupInfoPO groupInfoPO = GroupInfoPO.createGroupInfoPO();
                if (groupInfoPO != null) {
                    groupInfoPO.setGroupName(groupname);
                    groupInfoPO.save();
                    //这个组隶属于哪个Location，要建立Affiliation
                    LocationAffiliationPO locationAffiliationPO = new LocationAffiliationPO();
                    locationAffiliationPO.setRelativeType(Constants.CONTROL_TYPE_GROUP);
                    locationAffiliationPO.setRelativeId(groupInfoPO.getGroupId());
                    locationAffiliationPO.setRelativeName(groupname);
                    locationAffiliationPO.setLocationId(mLocationInfoPO.getLocationId());
                    locationAffiliationPO.setLocationName(mLocationInfoPO.getLocationName());
                } else {
                    Toast.makeText(getApplicationContext(), R.string.group_full, Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();

                if (mProvisionDevice.getDeviceType() == MeshConstants.DEVICE_TYPE_SWITCH) {
                    createSwitchControlGroupDialog();
                } else {
                    createSetProvisionGroupDialog();
                }
            }
        }

        private void createSwitchControlGroupDialog() {

        }
    }

    private void createSwitchSelectDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_switch);

        mSwitchControl = 0;

        builder.setSingleChoiceItems(new String[] {getResources().getString(R.string.switch_control_device),
                        getResources().getString(R.string.switch_control_group) },
                0,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int position) {

                        mSwitchControl = position;

                    }
                });

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

                if (mSwitchControl == 0) {

                    List<DeviceInfoPO> deviceInfoPOs = DeviceInfoPO.getDeviceListExceptSwitch();
                    if (deviceInfoPOs.size() > 0) {
                        createSwitchControlDeviceDialog();
                    } else {
                        createSwitchNODeviceDialog();
                    }

                } else if (mSwitchControl == 1) {

                    List<GroupInfoPO> groupInfoPOs = GroupInfoPO.getGroupList();
                    if (groupInfoPOs.size() > 0) {
                        createSwitchControlGroupDialog();
                    } else {
                        createSwitchNOGroupDialog();
                    }

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

    private void createSwitchNOGroupDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.no_available_group);

        builder.setMessage(R.string.no_available_group_content);

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                createSwitchSelectDialog();

            }
        });

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
    }

    private void createSwitchControlGroupDialog() {

        mSwitchControlGroupIndex = 0;

        List<GroupInfoPO> groupInfoPOs = GroupInfoPO.getGroupList();

        String[] groupsNames = new String[groupInfoPOs.size()];

        for (int i = 0; i < groupInfoPOs.size(); i++) {
            groupsNames[i] = groupInfoPOs.get(i).getGroupName();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setup_groups);
        builder.setCancelable(false);

//        final LinearLayout switchgroupLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_switchgroup,
//                null, false);
//        builder.setView(switchgroupLayout);

//        Button newgroupButton = (Button) switchgroupLayout.findViewById(R.id.button_switchgroup_new);
//        newgroupButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                createAddGroupDialog();
//            }
//        });
//
//        ListView listView = (ListView) switchgroupLayout.findViewById(R.id.listview_switchgroup);
//        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, groupsNames));
//        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//        listView.setItemChecked(0, true);
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
//
//                mSwitchControlGroupIndex = position;
//            }
//
//        });
        builder.setSingleChoiceItems(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_single_choice, groupsNames),0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSwitchControlGroupIndex = which;
            }
        });
        //条目选中
//        builder.setSingleChoiceItems(groupsNames, 0, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                mSwitchControlGroupIndex = which;
//            }
//        });
        builder.setNeutralButton(R.string.add_new_group, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createAddGroupDialog();
            }
        });

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

                List<GroupInfoPO> groupInfoPOs = GroupInfoPO.getGroupList();

                mProvisionDevice.setSwitchControlGroup(groupInfoPOs.get(mSwitchControlGroupIndex).getGroupId());

                createProvisionFinishDialog();
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

    private void createSwitchNODeviceDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.no_available_device);

        builder.setMessage(R.string.no_available_device_content);

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                createSwitchSelectDialog();

            }
        });

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
    }


    private void createSwitchControlDeviceDialog() {

        mSwitchControlDeiceIndex = 0;

        List<DeviceInfoPO> deviceInfoPOs = DeviceInfoPO.getDeviceListExceptSwitch();

        String[] deviceNames = new String[deviceInfoPOs.size()];

        for (int i = 0; i < deviceInfoPOs.size(); i++) {
            deviceNames[i] = deviceInfoPOs.get(i).getDeviceName();
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_switch);

        builder.setSingleChoiceItems(deviceNames,
                0,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int position) {

                        mSwitchControlDeiceIndex = position;
                    }
                });

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

                List<DeviceInfoPO> deviceInfoPOs = DeviceInfoPO.getDeviceListExceptSwitch();

                mProvisionDevice.setSwitchControlDevice(deviceInfoPOs.get(mSwitchControlDeiceIndex).getNodeId());

                createProvisionFinishDialog();
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

    private void createProvisionFinishDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.setup_finish);
        builder.setCancelable(false);

        final LinearLayout finishLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_finish, null, false);
        builder.setView(finishLayout);

        TextView addrTextView = (TextView) finishLayout.findViewById(R.id.textview_finish_addr);//mac地址
        TextView nameTextView = (TextView) finishLayout.findViewById(R.id.textview_finish_name);//设备名称
        TextView typeTextView = (TextView) finishLayout.findViewById(R.id.textview_finish_devicetype);//设备类型
        TextView groupsLabelTextView = (TextView) finishLayout.findViewById(R.id.textview_finish_groups_label);//所在位置
        TextView groupsTextView = (TextView) finishLayout.findViewById(R.id.textview_finish_groups);

        addrTextView.setText(mProvisionDevice.getMacAddress());
        nameTextView.setText(mProvisionDevice.getDeviceName());
//        typeTextView.setText();
        if (mProvisionDevice.getDeviceType() == MeshConstants.DEVICE_TYPE_SWITCH) {
            typeTextView.setText(MeshConstants.DEVICE_NAME[MeshConstants.DEVICE_TYPE_SWITCH]);

            if (mProvisionDevice.getSwitchControlDevice() > 0) {
                groupsLabelTextView.setText(R.string.control_device);

//                groupsTextView.setText(DeviceInfoPO.getDeviceInfoPO(mProvisionDevice.getSwitchControlDevice()).getDeviceName());

            } else if (mProvisionDevice.getSwitchControlGroup() > 0) {
                groupsLabelTextView.setText(R.string.control_group);

//                groupsTextView.setText(GroupInfoPO.getGroupInfoPO(mProvisionDevice.getSwitchControlGroup()).getGroupName());
            }

        } else if (mProvisionDevice.getDeviceType() > MeshConstants.DEVICE_TYPE_SWITCH) {
            if (mProvisionDevice.getDeviceType() < MeshConstants.DEVICE_TYPE_TEMINAL) {
                typeTextView.setText(MeshConstants.DEVICE_NAME[mProvisionDevice.getDeviceType()]);
            } else {
                typeTextView.setText(R.string.unknown_device_type);
            }

//            String groups = getResources().getString(R.string.label_ALL)
//                    + Constants.COMMA + Constants.SPACE + Constants.SPACE
//                    + getResources().getString(R.string.label_sensor);
//            groupsTextView.setText(groups);

        } else {
            if (mProvisionDevice.getDeviceType() < MeshConstants.DEVICE_TYPE_TEMINAL) {
                typeTextView.setText(MeshConstants.DEVICE_NAME[mProvisionDevice.getDeviceType()]);
            } else {
                typeTextView.setText(R.string.unknown_device_type);
            }

            String groups = getResources().getString(R.string.label_ALL);
            for (int groupId : mProvisionDevice.getGroupsId()) {
                groups += Constants.COMMA + Constants.SPACE + Constants.SPACE;
                groups += GroupInfoPO.getGroupInfoPO(groupId).getGroupName();
            }
//            groupsTextView.setText(groups);

        }
        groupsTextView.setText(mLocationInfoPO.getLocationName());//显示设备所在的位置


        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

                try {
                    mProgressDialog = new ProgressDialog(ProvisionDeviceActivity.this);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.setMessage(getResources().getString(R.string.setup_process) + Constants.PROGRESS);
                    mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface arg0) {

                        }
                    });

                    mProgressDialog.show();
                    if (getMeshService() != null) {
                        getMeshService().setupConfig(mProvisionDevice, mIProvisionCallback);
                    }
                } catch (Exception e) {
//                    CSLog.i(ProvisionFragment.class, e.toString());
                }
            }
        });

        builder.setNegativeButton(R.string.resetup, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                mProvisionDevice.setGroupsId(new ArrayList<Integer>());
                mProvisionDevice.setSwitchControlDevice(-1);
                mProvisionDevice.setSwitchControlGroup(-1);

                createSetProvisionNameDialog();
            }
        });

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
    }

//    private void createLeaveMeshWarnDialog() {
//
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(R.string.already_in_mesh);
//        builder.setCancelable(false);
//        builder.setPositiveButton(R.string.leave_mesh, new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface arg0, int arg1) {
//
//                // for test
//                // find device fragment and stop Advertise to leave Mesh
////                DeviceFragment fragment = (DeviceFragment) getActivity().getSupportFragmentManager()
////                        .findFragmentByTag(getActivity().getResources().getString(R.string.fragment_device));
////                fragment.setJoinedStatus(false);
//
//                if (getMeshService() != null) {
//                    getMeshService().leave(new ILeaveCallback() {
//                        @Override
//                        public void onSuccess() {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                }
//                            });
//                        }
//
//                        @Override
//                        public void onFailed(int errorCode) {
//
//                        }
//                    });
//                }
//
//                mScanResultAdapter.clear();
//                if (getMeshService() != null) {
//                    getMeshService().startScanning(Constants.SCAN_PERIOD, mStartScanCallback);
//                }
//                mScanning = true;
//                invalidateOptionsMenu();
//            }
//        });
//
//        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface arg0, int arg1) {
//
//            }
//        });
//
//        if (mDialog != null) {
//            mDialog.dismiss();
//        }
//        mDialog = builder.create();
//        mDialog.show();
//    }

    private void createReconnectDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reconnection);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {

                try {

                    mProgressDialog = new ProgressDialog(ProvisionDeviceActivity.this);
                    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialog.setCancelable(false);
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.setMessage(getResources().getString(R.string.setup_process) + Constants.PROGRESS);
                    mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface arg0) {

                        }
                    });

                    mProgressDialog.show();

                    if (getMeshService() != null) {
                        getMeshService().setupConfig(mProvisionDevice, mIProvisionCallback);
                    }
                } catch (Exception e) {
//                    CSLog.i(ProvisionFragment.class, e.toString());
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
}
