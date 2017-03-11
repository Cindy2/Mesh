package com.beesmart.blemesh.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.utils.AppManager;
import com.beesmart.blemesh.utils.PreferenceUtils;

/**
 * 准备设备提示界面
 */
public class PreparedDeviceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prepared_device);

        Button btNext = (Button) findViewById(R.id.bt_step1_next);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean keySetting = PreferenceUtils.getPrefBoolean(PreparedDeviceActivity.this, Constants.KEY_SETTING_FLAG, false);
                if (!keySetting) {//未设置MeshKey
                    startActivity(new Intent(PreparedDeviceActivity.this, SetupMeshKeyActivity.class));
                } else {//
                    startActivity(new Intent(PreparedDeviceActivity.this, ProvisionDeviceActivity.class));
                }
            }
        });
        AppManager.getAppManager().addActivity(PreparedDeviceActivity.this);
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
