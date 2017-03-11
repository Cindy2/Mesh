package com.beesmart.blemesh.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.adapter.SelectSceneDeviceAdapter;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;

public class SelectSceneDevicesActivity extends AppCompatActivity {
    SelectSceneDeviceAdapter deviceAdapter;

    List<LocationAffiliationPO> allLocationAffiliations;

    ArrayList<LocationAffiliationPO> selectedDevice = new ArrayList<>();

    private static final int REQUEST_CODE = 0X01;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_scene_devices);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ListView lv_select_devices = (ListView) findViewById(R.id.lv_select_device);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectedDevice.clear();

               //取出选中Device
                //传递到新建场景Activity
                for (int i = 0; i <allLocationAffiliations.size(); i++) {
                    if(deviceAdapter.isItemSelect.get(i)){
                        selectedDevice.add(allLocationAffiliations.get(i));
                    }
                }
                if(selectedDevice.isEmpty()){
                    Snackbar.make(view,"至少选择1个设备",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Log.d("SelectSceneDevices",">>>>>>选择设备数：："+selectedDevice.size());
                Intent intent = new Intent(SelectSceneDevicesActivity.this, NewSceneActivity.class);
                intent.putExtra(NewSceneActivity.TYPE,NewSceneActivity.TYPE_NEW);
                intent.putExtra(NewSceneActivity.PARAM_SELECT_DEVICE,selectedDevice);
                startActivityForResult(intent,REQUEST_CODE);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        allLocationAffiliations = LocationAffiliationPO.getAllLocationAffiliations();
        deviceAdapter = new SelectSceneDeviceAdapter(this, allLocationAffiliations);

        lv_select_devices.setAdapter(deviceAdapter);
        lv_select_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean isSelect = deviceAdapter.isItemSelect.get(position);
                deviceAdapter.isItemSelect.put(position, !isSelect);
                deviceAdapter.notifyDataSetChanged();
            }
        });

//       lv_select_devices.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE){
            setResult(RESULT_OK);
            finish();
        }
    }
}
