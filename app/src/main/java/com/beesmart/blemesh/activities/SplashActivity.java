package com.beesmart.blemesh.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.nxp.ble.meshlib.BLESupported;
import com.nxp.ble.meshlib.callback.IBLESupportedCallback;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.utils.AppManager;
import com.beesmart.blemesh.utils.PreferenceConstants;
import com.beesmart.blemesh.utils.PreferenceUtils;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 2000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        mContentView = findViewById(R.id.fullscreen_content);
//        ApkUtils.checkAppStatus(this);
        hide();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        bluetoothAdapter.enable();
        boolean enabled = bluetoothAdapter.isEnabled();
        if (!enabled){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,RESULT_FIRST_USER);

        }else {
            //测试先不开启检测
            checkBLEStatus();
        }
//        PreferenceUtils.setPrefInt(SplashActivity.this,Constants.NETWORK_FLAG,1);

        boolean isFirstUse = PreferenceUtils.getPrefBoolean(SplashActivity.this,PreferenceConstants.FIRST_USE, false);

        if (isFirstUse) {
            startActivity(new Intent(SplashActivity.this, GuideActivity.class));
            finish();
        } else {
            //是否已经加入或创建过Mesh 网络
            int enterkey = PreferenceUtils.getPrefInt(SplashActivity.this, Constants.NETWORK_FLAG, Constants.NETWORK_FLAG_NONE);
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            if (enterkey == Constants.NETWORK_FLAG_NONE) {
                startActivity(new Intent(SplashActivity.this, GuideActivity.class));
                finish();
            } else{
                startActivity(intent);
                finish();
            }
        }

    }



    /**
     * 检查手机是否支持BLE
     * @return
     */
    private boolean checkBLEStatus() {
        BLESupported.checkBLEStatus(this, new IBLESupportedCallback() {

            @Override
            public void onSuccess() {

                mHideHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {


                        boolean isFirstUse = PreferenceUtils.getPrefBoolean(SplashActivity.this,PreferenceConstants.FIRST_USE, false);

                        if (isFirstUse) {
                            startActivity(new Intent(SplashActivity.this, GuideActivity.class));
                            finish();
                        } else {
                            //是否已经加入或创建过Mesh 网络
                            int enterkey = PreferenceUtils.getPrefInt(SplashActivity.this, Constants.NETWORK_FLAG, Constants.NETWORK_FLAG_NONE);
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            if (enterkey == Constants.NETWORK_FLAG_NONE) {
                                startActivity(new Intent(SplashActivity.this, GuideActivity.class));
                                finish();
                            } else{
                                startActivity(intent);
                                finish();
                            }
                        }
                    }
                }, AUTO_HIDE_DELAY_MILLIS);
            }

            @Override
            public void onFailed(final int errorCode) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        createQuitAppDialog();
                    }
                });
            }
        });

        return false;
    }




    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.post(mHidePart2Runnable);
    }

    private void createQuitAppDialog() {
       AlertDialog quitAppDialog =  new AlertDialog.Builder(this).setTitle("Tips").setMessage("Sorry，your phone is not support BLE!")
                .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                        AppManager.getAppManager().AppExit(SplashActivity.this);
                    }
                }).create();
        quitAppDialog.setCanceledOnTouchOutside(false);
        quitAppDialog.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("ResultCode",">>>>>>>>>>>>"+resultCode+"");
        if (resultCode == RESULT_OK){
            checkBLEStatus();
        }else if (resultCode == RESULT_CANCELED){
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Tips").setMessage("The BEESMART App must run with Bluetooth enable.Please allow BEESMART access to the Bluetooth.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent,RESULT_FIRST_USER);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.create().show();
        }
    }
}
