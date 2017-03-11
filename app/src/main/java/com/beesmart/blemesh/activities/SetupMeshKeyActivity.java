package com.beesmart.blemesh.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.utils.AppManager;
import com.beesmart.blemesh.utils.PreferenceUtils;

/**
 * 创建Mesh网络第一步
 * 输入Mesh key
 */
public class SetupMeshKeyActivity extends AppCompatActivity {
    private EditText etInputMeshKey;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_key);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etInputMeshKey = (EditText) findViewById(R.id.et_input_key);

        Button btNext = (Button) findViewById(R.id.bt_submit);
        btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = etInputMeshKey.getText().toString();
                if (key.isEmpty()||key.length() < 6){
                    Snackbar.make(v,R.string.mesh_key_warning,Snackbar.LENGTH_SHORT).show();
                    return;
                }

                PreferenceUtils.setPrefString(SetupMeshKeyActivity.this, Constants.KEY, etInputMeshKey.getText().toString().trim());
                PreferenceUtils.setPrefBoolean(SetupMeshKeyActivity.this, Constants.KEY_SETTING_FLAG, true);



                startActivity(new Intent(SetupMeshKeyActivity.this, ProvisionDeviceActivity.class));
            }
        });
        AppManager.getAppManager().addActivity(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
