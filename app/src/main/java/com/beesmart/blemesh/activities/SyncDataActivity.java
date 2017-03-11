package com.beesmart.blemesh.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.nxp.utils.crypto.PrivateData;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.utils.PreferenceUtils;

public class SyncDataActivity extends AppCompatActivity {
    private Button bt_receiveData;
    private EditText et_key;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_data);

        getSupportActionBar().setHomeButtonEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        et_key = (EditText) this.findViewById(R.id.et_input_key);
        bt_receiveData = (Button) this.findViewById(R.id.bt_start_sync);
        bt_receiveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(et_key.getText().toString().isEmpty()){

                   return ;
                }
                String provisionkey = PrivateData.createProvisionKey(et_key.getText().toString());

                try {
                    PrivateData.saveProvisionKey(SyncDataActivity.this.getApplicationContext(), provisionkey);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                PreferenceUtils.setPrefInt(SyncDataActivity.this, Constants.NETWORK_FLAG, 2);
                startActivity(new Intent(SyncDataActivity.this, ReceiveDBActivity.class));
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
