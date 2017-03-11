package com.beesmart.blemesh.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.nxp.ble.meshlib.MeshService;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceOperationPO;
import com.nxp.utils.po.GroupOperationPO;

import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.adapter.SceneControlAdapter;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.SceneDeviceOperationPO;
import com.beesmart.blemesh.dao.po.SceneGroupOperationPO;
import com.beesmart.blemesh.dao.po.SceneInfoPO;

/**
 * 新建情景界面，编辑场景，保存场景
 */
public class NewSceneActivity extends AppCompatActivity {
    public static final String TYPE = "type";
    public static final int TYPE_NEW = 0;
    public static final int TYPE_EDIT = 1;

    public static final String PARAM_EDIT = "sceneId";
    private ListView lvDevices;
    //操作mesh device 的服务
    public MeshService mMeshService;

    public static final String PARAM_SELECT_DEVICE = "selected_devices";

    private List<LocationAffiliationPO> devices;

    SceneInfoPO sceneInfoPO;
    private int type = 0;
    Dialog mDialog;
    private int sceneId;//用户情景编辑
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_scene);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        type = getIntent().getIntExtra(TYPE,0);
        if(type == TYPE_EDIT){
            setTitle(getString(R.string.scene_edit));
            sceneId = getIntent().getIntExtra(PARAM_EDIT,-1);
            Log.e("Scene edit","SceneId>>>>"+sceneId);
        }
        devices = (List<LocationAffiliationPO>) getIntent().getSerializableExtra(PARAM_SELECT_DEVICE);
        lvDevices = (ListView) findViewById(R.id.lv_new_scene_device);
        if (getMeshService() == null){
            final Intent meshServiceIntent = new Intent(this, MeshService.class);
            bindService(meshServiceIntent, mMeshServiceConnection, Context.BIND_AUTO_CREATE);
        }

        //save the scene operation data

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.save).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1){
            //
            createSetSceneNameDialog();
        }
        return super.onOptionsItemSelected(item);
    }
    private void createSetSceneNameDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = View.inflate(this,R.layout.dialog_addgroup,null);
        builder.setTitle("Name Scene").setView(view);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        if (mDialog != null){
            mDialog.dismiss();
            mDialog = null;
        }
        mDialog = builder.create();
        mDialog.show();
        ((AlertDialog)mDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) view.findViewById(R.id.edittext_groupname);
                String sceneName = editText.getText().toString();
                if (!sceneName.isEmpty()){
                    if(!SceneInfoPO.isSceneNameExists(sceneName)){
                        sceneInfoPO = SceneInfoPO.createSceneInfoPO();
                        sceneInfoPO.setSceneName(sceneName);
                        sceneInfoPO.save();
                        //开始创建Scene数据
                        createSceneData();
                        mDialog.dismiss();
                        Toast.makeText(NewSceneActivity.this, "Scene save successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }else{
                        Snackbar.make(editText,"Scene name exists!",Snackbar.LENGTH_SHORT).show();
                    }
                }else{
                    Snackbar.make(editText,"Scene name can not be empty!",Snackbar.LENGTH_SHORT).show();
                }
            }
        });

    }

    //create Scene Data
    private void createSceneData() {
        Log.d("createSceneData", "start createData" );

        //开启事务
        ActiveAndroid.beginTransaction();
        try {
            for (int i = 0; i < devices.size(); i++) {
                if (devices.get(i).getRelativeType() == Constants.CONTROL_TYPE_DEVICE){
                    DeviceOperationPO deviceOperationPO = DeviceOperationPO.getDeviceOperationPO(devices.get(i).getRelativeId());
                    //判断这个数据是否数据库已经有了
                    SceneDeviceOperationPO sceneDeviceOperationPO = SceneDeviceOperationPO.getDeviceOperationPO(sceneInfoPO.getSceneId(),deviceOperationPO.getDeviceId());
                    if(sceneDeviceOperationPO == null){
                        sceneDeviceOperationPO = SceneDeviceOperationPO.createSceneDeviceOperationPO(devices.get(i).getRelativeId(), devices.get(i).getRelativeType());
                    }

                    sceneDeviceOperationPO.setSceneId(sceneInfoPO.getSceneId());
                    sceneDeviceOperationPO.setDevicedata1(deviceOperationPO.getValue1());
                    sceneDeviceOperationPO.setDevicedata2(deviceOperationPO.getValue2());
                    sceneDeviceOperationPO.setDevicedata3(deviceOperationPO.getValue3());
                    sceneDeviceOperationPO.save();
                }else if (devices.get(i).getRelativeType() == Constants.CONTROL_TYPE_GROUP){
                    GroupOperationPO groupOperationPO = GroupOperationPO.getGroupOperationPO(devices.get(i).getRelativeId());
                    SceneGroupOperationPO sceneGroupOperationPO = SceneGroupOperationPO.getGroupOperationPO(sceneInfoPO.getSceneId(), groupOperationPO.getGroupId());
                    if (sceneGroupOperationPO == null) {//判断这个数据是否数据库已经有了
                        sceneGroupOperationPO = SceneGroupOperationPO.createSceneGroupOperationPO(groupOperationPO.getGroupId(), Constants.CONTROL_TYPE_GROUP);
                    }
                    sceneGroupOperationPO.setSceneId(sceneInfoPO.getSceneId());
                    sceneGroupOperationPO.setGroupdata1(groupOperationPO.getValue1());
                    sceneGroupOperationPO.setGroupdata2(groupOperationPO.getValue2());
                    sceneGroupOperationPO.setGroupdata3(groupOperationPO.getValue3());
                    sceneGroupOperationPO.save();
                }else{

                }
            }
            ActiveAndroid.setTransactionSuccessful();

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            ActiveAndroid.endTransaction();

        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    protected void onDestroy() {
        super.onDestroy();

        if (mMeshService != null) {
            unbindService(mMeshServiceConnection);
        }
    }

    public final ServiceConnection mMeshServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            CSLog.i(MainActivity.class, "Mesh service onServiceConnected ");
            mMeshService = ((MeshService.LocalBinder) service).getService();
            //start Join
            SceneControlAdapter adapter = new SceneControlAdapter(NewSceneActivity.this, mMeshService, devices);
            lvDevices.setAdapter(adapter);
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
}
