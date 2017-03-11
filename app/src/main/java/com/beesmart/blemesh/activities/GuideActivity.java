package com.beesmart.blemesh.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.utils.AppManager;

/**
 * 用户向导，选择Create or Join
 */
public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        Button btCreateMesh = (Button) findViewById(R.id.bt_go_create_mesh);
        Button btJoinMesh = (Button) findViewById(R.id.bt_go_join_mesh);

        btCreateMesh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(GuideActivity.this, PreparedDeviceActivity.class));

            }
        });

        btJoinMesh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Snackbar.make(v,"Join exits Mesh network",Snackbar.LENGTH_SHORT).show();
                startActivity(new Intent(GuideActivity.this,SyncDataActivity.class));
            }
        });

        AppManager.getAppManager().addActivity(this);
//
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
