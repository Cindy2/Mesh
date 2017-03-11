package com.beesmart.blemesh.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nxp.ble.meshlib.CCTWUtil;
import com.nxp.ble.meshlib.MeshService;
import com.nxp.ble.meshlib.callback.IJoinCallback;
import com.nxp.ble.meshlib.callback.ILeaveCallback;
import com.nxp.ble.meshlib.callback.IOTAResponseCallback;
import com.nxp.ble.meshlib.callback.IUpdateStatusCallback;
import com.nxp.ble.meshlib.operation.OperationInMesh;
import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.crypto.PrivateData;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceInfoPO;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.MyApplication;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.dao.po.SceneInfoPO;
import com.beesmart.blemesh.fragments.DeviceControlFragment;
import com.beesmart.blemesh.fragments.MainControlFragment;
import com.beesmart.blemesh.fragments.SceneFragment;
import com.beesmart.blemesh.utils.AppManager;
import com.beesmart.blemesh.utils.DeviceStatusObserver;
import com.beesmart.blemesh.utils.PreferenceUtils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, DeviceControlFragment.OnListFragmentInteractionListener {
    public static String PARAM_NETWORK_FLAG = "param_network_flag";

    public MeshService mMeshService;

    private static final String MAIN_CONTROL = "MainControl";

    private int mJoinStatus = 0;

    public static final int NOT_JOIN = 0;

    public static final int JOINING = 1;

    public static final int JOINED = 2;
    public static final int JOINED_SUCCESS = 3;
    public static final int JOINED_FAILED = 4;
    public static final int JOINED_TIMEOUT =5;

    private boolean isActivityOnFront = false;
    /**Use to JoinMesh*/

    private int mJoinCount = 0;
    public static final int DEFALUT_JOIN_COUNT = 3;
    /**Default antuo join delay millsecond*/
    public static long DEFAULT_AUTO_JOIN_DELAY = 0l;

    private ProgressDialog mJoinProgress;

    boolean allDeviceOn = false;
    private boolean cancelByUser = false;
    private DrawerLayout drawer;
    OperationInMesh mOperationInMesh;
//    ListView locationList;
    public int getJoinStatus(){
        return mJoinStatus;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_1);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        locationList = (ListView) findViewById(R.id.lv_main_location);
//        locationList.setAdapter(new LocationAdapter(this, LocationInfoPO.getLocationInfoPos(true)));
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawer.setDrawerListener(toggle);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setNavMenuStatus();
//        drawer.openDrawer(GravityCompat.START);
        //一进来就默认显示控制设备

        mJoinProgress = new ProgressDialog(this);
        mJoinProgress.setMessage("Joining the mesh network !");
        mJoinProgress.setIndeterminate(false);
        mJoinProgress.setCanceledOnTouchOutside(false);
        mJoinProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelByUser = true;
                Toast.makeText(MainActivity.this, "Connect canceled", Toast.LENGTH_SHORT).show();
            }
        });

    }
    protected Handler mJoinMeshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){

                case JOINED_SUCCESS:
                    Toast.makeText(MainActivity.this, "Join Successful", Toast.LENGTH_SHORT).show();
                    mJoinStatus = JOINED;
                    //reset mjoinCount
                    mJoinCount = 0;
                    invalidateOptionsMenu();
                    mJoinProgress.dismiss();
                    cancelByUser =false;
                    break;
                case JOINED_FAILED:
//                    Toast.makeText(MainActivity.this, "Join Failed", Toast.LENGTH_SHORT).show();
                    mJoinStatus = NOT_JOIN;
                    invalidateOptionsMenu();

                    //judgde whether Activity is actived，if not, no need to rejoin the mesh
                    if (isActivityOnFront){
                        if (!cancelByUser){
                            if (!mJoinProgress.isShowing()){
                                mJoinProgress.show();
                            }
                            doJoinMesh();

                        }
                    }
                    break;
                case JOINED_TIMEOUT:
//                    Toast.makeText(MainActivity.this, "Join Timeout", Toast.LENGTH_SHORT).show();

                    mJoinStatus = NOT_JOIN;
                    //reJoin the mesh
                    if (isActivityOnFront){
                        if (!cancelByUser){
                            if (!mJoinProgress.isShowing()){
                                mJoinProgress.show();
                            }
                            doJoinMesh();

                        }
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };
    protected void onResume() {
        super.onResume();
        cancelByUser = false;
        isActivityOnFront = true;
        if (mMeshService == null) {
            CSLog.i(MainActivity.class, "start bind mesh service");
            final Intent meshServiceIntent = new Intent(MainActivity.this, MeshService.class);
            bindService(meshServiceIntent, mMeshServiceConnection, Context.BIND_AUTO_CREATE);
        }else{
            if (mJoinStatus == NOT_JOIN){
                if(DeviceInfoPO.getDeviceCount() == 0) {

                    Toast.makeText(MainActivity.this, "Not any device,please add devices first!", Toast.LENGTH_SHORT).show();
                }else{
                    doJoinMesh();
                }
            }
        }

    }
    /**Auto join Mesh*/
    private synchronized void doJoinMesh(){
//        if (!mJoinProgress.isShowing()) {
//            mJoinProgress.show();
//        }
        mJoinStatus = JOINING;
//        invalidateOptionsMenu();
        CSLog.d(this.getClass(), ">>>>>>>>>>>start auto Join");
        mJoinCount++;
        CSLog.d(this.getClass(), "Join count>>>>>" + mJoinCount);

        //ReJoin the mesh until the mJoinCount reach DEFALUT_JOIN_COUNT

        if (getMeshService() != null) {
            mMeshService.join(Constants.JOIN_TIMEOUT, meshJoinCallback);
        }


    }

    private void createJoinFiledDialog() {
        //TODO
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tips);
        builder.setMessage(R.string.join_failed_message);
        builder.setPositiveButton(R.string.join_again, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mJoinCount = 0;
                doJoinMesh();
            }
        });
        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                AppManager.getAppManager().finishAllActivity();
            }
        });
        builder.create().show();
    }


    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if(mJoinStatus == NOT_JOIN) {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setTitle(R.string.menu_disconnect);
        }else if(mJoinStatus == JOINING){
            menu.getItem(0).setVisible(false);
//            menu.getItem(1).setActionView(R.layout.actionbar_progress);
//            menu.getItem(1).setVisible(true);
            menu.getItem(2).setTitle(R.string.connecting);
        }else if(mJoinStatus == JOINED){
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setTitle(R.string.connected);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_join) {
            if (mJoinStatus == NOT_JOIN){
                cancelByUser = false;
                mJoinStatus = JOINING;
                invalidateOptionsMenu();
//                getMeshService().join(Constants.JOIN_TIMEOUT, meshJoinCallback);
//                doJoinMesh();
                mJoinMeshHandler.sendEmptyMessage(JOINED_FAILED);
            }else if(mJoinStatus == JOINING){
                Toast.makeText(MainActivity.this, "Joining! Please Wait!", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this, "Joined", Toast.LENGTH_SHORT).show();

            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**Join Mesh Callback*/
    private IJoinCallback meshJoinCallback = new IJoinCallback() {
        @Override
        public void onTimeout() {
            mJoinMeshHandler.sendEmptyMessage(JOINED_TIMEOUT);
        }

        @Override
        public void onSuccess() {
            mJoinMeshHandler.sendEmptyMessage(JOINED_SUCCESS);

        }

        @Override
        public void onFailed(int i) {
            System.out.println("Join failed code>>>>>:"+i);
            mJoinMeshHandler.sendEmptyMessage(JOINED_FAILED);

        }

        @Override
        public void onUpdate(int i, int i1) {
            Log.d("meshJoinCallback","onUpdate is called");
        }
    };

    /**
     * Mesh状态同步回调
     */
    private IUpdateStatusCallback deviceStatusUpdateCallback = new IUpdateStatusCallback() {

        @Override
        public void onDisconnect(int i) {
            mJoinMeshHandler.sendEmptyMessage(JOINED_FAILED);

        }

        @Override
        public void onUpdate(final int objType,final int objId) {
            Log.i("deviceStatusCallback","原始状态回调-type："+objType+" ; Id:"+objId);
            mJoinMeshHandler.post(new Runnable() {
                @Override
                public void run() {
                    DeviceStatusObserver.notifyStatusUpdate(objType,objId);
                }
            });

        }
    };

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_main:
                Fragment mainControl = getSupportFragmentManager().findFragmentByTag(MAIN_CONTROL);
                if (mainControl == null) {

                    getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, new MainControlFragment(), MAIN_CONTROL).commit();
                } else {
                    //show MainControlFragment
//                    Toast.makeText(MainActivity.this, "Mesh not join,please wait!", Toast.LENGTH_SHORT).show();
                    getSupportFragmentManager().beginTransaction().show(mainControl).commit();
                }
                setTitle(item.getTitle());
                closeDrawer();

                break;
            case R.id.nav_add_devices:
                doLeaveMesh();
                startActivity(new Intent(this, PreparedDeviceActivity.class));
                closeDrawer();
                isActivityOnFront = false;
                break;
            case R.id.nav_send_data:
                doLeaveMesh();
                startActivity(new Intent(this, SendDBActivity.class));
                closeDrawer();
                isActivityOnFront = false;
                break;
            case R.id.nav_receive_data:
                doLeaveMesh();
                startActivity(new Intent(this, ReceiveDBActivity.class));
                closeDrawer();
                isActivityOnFront = false;
                //接收数据
                break;
            case R.id.nav_exit:
                //情景

                closeDrawer();
                finish();
                AppManager.getAppManager().finishAllActivity();
                break;
            case R.id.nav_scene:
                //情景
                setTitle(item.getTitle());
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, new SceneFragment(), "Scene").commit();
                closeDrawer();
                break;
            case R.id.nav_device_all_on://测试用
                //全部灯亮
                if (getMeshService() != null) {
                    CCTWUtil cctwUtil = new CCTWUtil(this, getMeshService());
                    cctwUtil.changeGroupParameters(0, !allDeviceOn, MeshConstants.CONTROL_FLAG_REQ_BROADCAST
                            | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT
                            | MeshConstants.CONTROL_FLAG_RESP_BROADCAST);
                    allDeviceOn = !allDeviceOn;
                    if (allDeviceOn) {
                        item.setTitle("All Off");
                    } else {
                        item.setTitle("All On");

                    }
                }
                break;
            case R.id.nav_clear_data:
                closeDrawer();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Warning");
                builder.setMessage("This operation will clear all local data! Are you sure to do that?");
                builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((MyApplication)getApplication()).clearDatabase();

                        PreferenceUtils.setPrefInt(MainActivity.this,Constants.NETWORK_FLAG, 0);
                        PreferenceUtils.setPrefString(MainActivity.this,Constants.KEY, Constants.EMPTY);
                        PreferenceUtils.setPrefBoolean(MainActivity.this,Constants.KEY_SETTING_FLAG, false);
                        PreferenceUtils.setPrefInt(MainActivity.this,Constants.GROUP_NAME_COUNT, 0);
                        PreferenceUtils.setPrefInt(MainActivity.this,Constants.DEVICE_NAME_COUNT, 0);
                        PreferenceUtils.setPrefInt(MainActivity.this,Constants.SWITCH_NAME_COUNT, 0);
                        //清除原先的MeshKey
                        PrivateData.removeProvisionKey(MainActivity.this);
                        startActivity(new Intent(MainActivity.this,GuideActivity.class));
                        finish();
                    }
                });
                builder.setNegativeButton(R.string.Cancel,null);
                builder.create().show();
                break;
            case R.id.nav_ota:
                if (mJoinStatus == JOINED) {
                    // if in join mode;
                    createStartOTADialog();
                } else {
                    createCannotOTADialog();
                }
                break;
        }

        return true;
    }

    private void createStartOTADialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.start_OTA);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

                if (mOperationInMesh == null) {
                    mOperationInMesh = new OperationInMesh(getApplicationContext(), getMeshService());
                }

                mOperationInMesh.startOTA(new IOTAResponseCallback() {

                    @Override
                    public void onTimeOut() {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, R.string.start_OTA_timeout, Toast.LENGTH_SHORT).show();
                            }
                        });

                    }

                    @Override
                    public void onSuccess(final int nodeId) {
                        cancelByUser = true;
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
                                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                dialog.setCancelable(false);
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.setMessage(getString(R.string.waiting_device_reboot));
                                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface arg0) {

                                        DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(nodeId);

                                        if (deviceInfoPO != null && deviceInfoPO.getDeviceFlag() != 1) {
                                            Intent intent = new Intent(MainActivity.this, DeviceOTAActivity.class);
                                            intent.putExtra(DeviceOTAActivity.EXTRAS_DEVICE_NAME,
                                                    deviceInfoPO.getDeviceName());
                                            // intent.putExtra(DeviceOTAActivity.EXTRAS_DEVICE_ADDRESS,
                                            //		deviceInfoPO.getDeviceNo());
                                            intent.putExtra(DeviceOTAActivity.EXTRAS_DEVICE_ADDRESS,
                                                    getMeshService().getOTADeviceAddress());
                                            CSLog.i(MainActivity.class, "join address : " + getMeshService().getOTADeviceAddress());

                                            unbindMeshService();

                                            startActivityForResult(intent, 0);
                                        } else {
                                            Toast.makeText(MainActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                                dialog.show();

                                new Thread(new Runnable() {

                                    @Override
                                    public void run() {

                                        mJoinStatus = NOT_JOIN;

                                        CSLog.i(MainActivity.class, "sleep 1500 ms");
                                        try {
                                            Thread.sleep(1500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        CSLog.i(MainActivity.class, "stopAdvertising");
                                        getMeshService().stopAdvertising();

                                        CSLog.i(MainActivity.class, "sleep second 3000 ms");
                                        try {
                                            Thread.sleep(3500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        dialog.dismiss();

                                    }
                                }).start();

                            }
                        });
                    }

                    @Override
                    public void onFailed() {
                        CSLog.i(MainActivity.class, "startOTAInMesh onFailed");
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, R.string.start_OTA_failed, Toast.LENGTH_SHORT).show();

                            }
                        });
                    }

                });

            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });

        builder.create().show();
    }
    private void createCannotOTADialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cannot_OTA);
        builder.setMessage(R.string.join_first);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }

        });

        builder.create().show();
    }

    public void closeDrawer() {
        if (drawer.isDrawerOpen(GravityCompat.START)){

            drawer.closeDrawer(GravityCompat.START);
        }
    }

    public void setNavMenuStatus(){
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        int enterkey = PreferenceUtils.getPrefInt(this, Constants.NETWORK_FLAG, Constants.NETWORK_FLAG_NONE);
        if (enterkey == Constants.NETWORK_FLAG_CERATE) {
            navigationView.getMenu().findItem(R.id.nav_receive_data).setVisible(false);
        } else if (enterkey == Constants.NETWORK_FLAG_JOIN) {
            navigationView.getMenu().findItem(R.id.nav_send_data).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_add_devices).setVisible(false);
        }else{
            navigationView.getMenu().findItem(R.id.nav_receive_data).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_send_data).setVisible(false);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (getMeshService()!=null){
//            getMeshService().leave(new ILeaveCallback() {
//                @Override
//                public void onSuccess() {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(MainActivity.this, "App has Leaved Mesh!", Toast.LENGTH_SHORT).show();
//                            mJoinStatus = NOT_JOIN;
//                            invalidateOptionsMenu();
//                        }
//                    });
//                }
//
//                @Override
//                public void onFailed(int i) {
//
//                }
//            });
        }
        isActivityOnFront = false;
    }

    protected void onDestroy() {
        super.onDestroy();

        if (mMeshService != null) {

            doLeaveMesh();
            isActivityOnFront = false;
            mMeshService.removeUpdateStatusCallback(deviceStatusUpdateCallback);
            unbindService(mMeshServiceConnection);
        }
    }

    public void doLeaveMesh() {
        if (mMeshService != null) {
            mMeshService.leave(new ILeaveCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "App has leaved mesh!", Toast.LENGTH_SHORT).show();
                            mJoinStatus = NOT_JOIN;
                        }
                    });
                }

                @Override
                public void onFailed(int i) {

                }
            });
        }
    }

    public final ServiceConnection mMeshServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            CSLog.i(MainActivity.class, "Mesh service onServiceConnected ");
            mMeshService = ((MeshService.LocalBinder) service).getService();

            //先绑定了Service，才能显示设备界面
            getSupportFragmentManager().beginTransaction().replace(R.id.main_content_fragment, new MainControlFragment(), MAIN_CONTROL).commit();
            getMeshService().addUpdateStatusCallback(deviceStatusUpdateCallback);

            //start Join
            if(DeviceInfoPO.getDeviceCount() == 0) {
                Toast.makeText(MainActivity.this, "No any device,please add devices first!", Toast.LENGTH_SHORT).show();
            }else{
                if (!mJoinProgress.isShowing()){
                    mJoinProgress.show();
                }
                doJoinMesh();
            }
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


    @Override
    public void onListFragmentInteraction(SceneInfoPO item) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
