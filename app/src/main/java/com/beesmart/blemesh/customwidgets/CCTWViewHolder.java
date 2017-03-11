package com.beesmart.blemesh.customwidgets;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.nxp.ble.meshlib.CCTWUtil;
import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.DeviceOperationPO;
import com.nxp.utils.po.GroupInfoPO;
import com.nxp.utils.po.GroupOperationPO;

import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.utils.CommonUtils;
import com.beesmart.blemesh.utils.DeviceStatusObserver;

/**
 * Created by alphawong on 2016/3/24.
 * CCTW Controller
 */
public class CCTWViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener,CompoundButton.OnCheckedChangeListener,DeviceStatusObserver.Observer{
    public static final int CONTROL_IN_LOCATION = 0X11;
    public static final int CONTROL_IN_GROUP = 0X22;
    public View itemView;
    public ImageView iv_devicelogo;
    public TextView tv_deviceName;
    public TextView tv_intensity;
    public TextView tv_color;
    public SeekBar sb_intensity;
    public SeekBar sb_color;
    public Switch aSwitch_onoff;

    public int controlType = -1;

    private static final int DIVID_VALUE = 5;
    private CCTWUtil mCctwUtil;//Mesh control

    private Activity activity;//Refreshing UI
    public CCTWViewHolder(View view, CCTWUtil mCctwUtil, Context context) {
        super(view);
        this.itemView = view;
        this.sb_color = (SeekBar) view.findViewById(R.id.sb_item_color);
        this.sb_intensity = (SeekBar) view.findViewById(R.id.sb_item_intensity);
        this.tv_color = (TextView) view.findViewById(R.id.tv_item_color);
        this.tv_intensity = (TextView) view.findViewById(R.id.tv_item_intensity);
        this.tv_deviceName = (TextView) view.findViewById(R.id.tv_item_device_name);
        this.iv_devicelogo = (ImageView) view.findViewById(R.id.iv_item_device_type);
        this.aSwitch_onoff = (Switch) view.findViewById(R.id.switch_item_device_onoff);
        this.sb_color.setOnSeekBarChangeListener(this);
        this.sb_intensity.setOnSeekBarChangeListener(this);
        this.aSwitch_onoff.setOnCheckedChangeListener(this);
        this.mCctwUtil =mCctwUtil;
//        this.mCctwUtil = new CCTWUtil(context,((MainActivity)context).getMeshService());
        activity = (Activity)context;
    }
    //设备类型
    public int type;
    //
    private GroupOperationPO groupOperationPO;

    private DeviceOperationPO deviceOperationPO;

    public int relativeId;

    /**
     * 设置这个是控制设备还是控制组
     * @param relativeType
     * @param relativeId
     */
    public void setTypeAndId(int relativeType,int relativeId){
        this.type = relativeType;
        this.relativeId = relativeId;
        if (type == Constants.CONTROL_TYPE_DEVICE){
//            deviceOperationPO = DeviceOperationPO.getDeviceOperationPO(relativeId);
            refreshDeviceUI();
        }else{
//            groupOperationPO = GroupOperationPO.getGroupOperationPO(relativeId);
            refreshGroupUI();
        }
    }



    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.i("seekBar Control", "onProgressChanged" + seekBar.getProgress());
        if(aSwitch_onoff.isChecked()) {//only device on, will send cmd to the mesh
            if (fromUser) {
                if (seekBar == this.sb_color) {

                    tv_color.setText(CommonUtils.getCCTWColor(progress));
                } else if (seekBar == this.sb_intensity) {
                    tv_intensity.setText(progress + "%");
                }
//                if (progress % DIVID_VALUE == 0) {

//                    sendCommand(true);
//                    if (type == Constants.CONTROL_TYPE_DEVICE) {//设备
//
//                        mCctwUtil.changeDeviceParameters(deviceOperationPO.getDeviceId(), true, (byte) sb_intensity.getProgress(), (byte) sb_color.getProgress(),
//                                MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST,
//                                iCommandResponseCallback);
//
//                    } else if (type == Constants.CONTROL_TYPE_GROUP) {//组
//                        mCctwUtil.changeGroupParameters(groupOperationPO.getGroupId(), true, (byte) sb_intensity.getProgress(), (byte) sb_color.getProgress(),
//                                MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST);
//                    }
//                }

            }
        }
    }

    private void sendCommand(boolean isCheck) {
        if (type == Constants.CONTROL_TYPE_DEVICE) {//设备

            mCctwUtil.changeDeviceParameters(deviceOperationPO.getDeviceId(), isCheck, (byte) sb_intensity.getProgress(), (byte) sb_color.getProgress(),
                    MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST,
                    iCommandResponseCallback);

        } else if (type == Constants.CONTROL_TYPE_GROUP) {//组
            mCctwUtil.changeGroupParameters(groupOperationPO.getGroupId(), isCheck, (byte) sb_intensity.getProgress(), (byte) sb_color.getProgress(),
                    MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST);
        }
    }

    /**
     * 命令发送回调
     */
    private ICommandResponseCallback iCommandResponseCallback = new ICommandResponseCallback() {
        @Override
        public void onTimeOut() {
            Log.i("CommandResponseCallback",">>>>cmd send Timeout");
        }

        @Override
        public void onSuccess() {
            Log.i("CommandResponseCallback",">>>>cmd send successful");
        }

        @Override
        public void onFailed() {
            Log.i("CommandResponseCallback",">>>>cmd send Failed");
        }
    };

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i("seekBar Control", "onStartTrackingTouch" + seekBar.getProgress());
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //TODO 保存数据
        if(aSwitch_onoff.isChecked()){
            saveOperationData();
            if (type == Constants.CONTROL_TYPE_DEVICE) {//设备

                mCctwUtil.changeDeviceParameters(deviceOperationPO.getDeviceId(), true, (byte) sb_intensity.getProgress(), (byte) sb_color.getProgress(),
                        MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST,
                        iCommandResponseCallback);

            } else if (type == Constants.CONTROL_TYPE_GROUP) {//组
                mCctwUtil.changeGroupParameters(groupOperationPO.getGroupId(), true, (byte) sb_intensity.getProgress(), (byte) sb_color.getProgress(),
                        MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST);
            }
        }

    }

    /**Save Device or Group status Data*/
    private void saveOperationData() {
        if (type == Constants.CONTROL_TYPE_DEVICE){
            deviceOperationPO.setValue1(aSwitch_onoff.isChecked()?1:0);
            deviceOperationPO.setValue2(sb_color.getProgress());
            deviceOperationPO.setValue3(sb_intensity.getProgress());
            deviceOperationPO.save();
        }else if(type == Constants.CONTROL_TYPE_GROUP){
            groupOperationPO.setValue1(aSwitch_onoff.isChecked()?1:0);
            groupOperationPO.setValue2(sb_color.getProgress());
            groupOperationPO.setValue3(sb_color.getProgress());
            groupOperationPO.save();
            int groupId = groupOperationPO.getGroupId();
            GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(groupId);
            List<DeviceInfoPO> deviceInfoPOList = groupInfoPO.getDeviceListAffiliationsExceptSwitch();
            for (DeviceInfoPO po : deviceInfoPOList) {
                DeviceOperationPO deviceOperationPO = DeviceOperationPO.getDeviceOperationPO(po.getNodeId());
                deviceOperationPO.setValue1(aSwitch_onoff.isChecked()?1:0);
                deviceOperationPO.setValue2(sb_color.getProgress());
                deviceOperationPO.setValue3(sb_intensity.getProgress());
                deviceOperationPO.save();
            }

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked){
            //Device on
            this.sb_color.setEnabled(true);
            this.sb_intensity.setEnabled(true);
        }else{//if device off. it can't be controlled
            //Device off
            this.sb_color.setEnabled(false);
            this.sb_intensity.setEnabled(false);

        }
        Log.e("isUpdateStatus",">>>>>:"+isUpdate);
        if (!isUpdate) {
            if (type == Constants.CONTROL_TYPE_DEVICE) {
                Log.e("CONTROL_TYPE_DEVICE",">>>>>:"+type);
                mCctwUtil.changeDeviceParameters(deviceOperationPO.getDeviceId(), isChecked,
                        MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST,
                        iCommandResponseCallback);
            } else if (type == Constants.CONTROL_TYPE_GROUP) {
                Log.e("CONTROL_TYPE_GROUP",">>>>>:"+type);
                mCctwUtil.changeGroupParameters(groupOperationPO.getGroupId(), isChecked,
                        MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST);
            }
//            sendCommand(isChecked);
            //保存状态数据
            saveOperationData();
        }
        isUpdate = false;
    }


    @Override
    public void update(int objType, int objId) {
        //
        Log.d("CCTWController", "update: objid>>>>>" + objId);
        Log.d("CCTWController", "update: relativeId>>>>>" + relativeId);
        if (objType == Constants.CONTROL_TYPE_DEVICE && objId == relativeId) {
            Log.d("CCTWController", "update: >>>>>" + objId);
            isUpdate = true;
            if (type == Constants.CONTROL_TYPE_DEVICE) {
                Log.d("CCTWController", "refreshDeviceUI: >>>>>" + objId);
                refreshDeviceUI();

            } else {
                Log.d("CCTWController", "refreshGroupUI: >>>>>" + objId);

                refreshGroupUI();

            }
        }else if(objType == Constants.CONTROL_TYPE_GROUP ) {
            Log.d("CCTWController", "control group : >>>>>" + objId);
            if(type == Constants.CONTROL_TYPE_GROUP){
                refreshGroupUI();
            }

            GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(objId);
            if (groupInfoPO != null && groupInfoPO.containDevice(relativeId)) {
                refreshDeviceUI();
            }
        }
        isUpdate = false;
    }

    public int getRelativeId() {
        return relativeId;
    }
    private boolean isUpdate =false;
    public void refreshDeviceUI(){
        Log.e("refreshDeviceUI","refreshDeviceUI was called");
        deviceOperationPO = DeviceOperationPO.getDeviceOperationPO(relativeId);
        if(deviceOperationPO == null){
            deviceOperationPO = DeviceOperationPO.createOperationPO(relativeId);
        }
        Log.e("refreshDATA","isCheck:"+deviceOperationPO.getValue1()+";color:"+deviceOperationPO.getValue2()+";intensity:"+deviceOperationPO.getValue3());

        this.sb_intensity.setProgress(deviceOperationPO.getValue3());
        this.sb_color.setProgress(deviceOperationPO.getValue2());
        this.tv_color.setText(CommonUtils.getCCTWColor(deviceOperationPO.getValue2()));
        this.tv_intensity.setText(deviceOperationPO.getValue3() + "%");

        if (deviceOperationPO.getValue1()==0){//关
            this.sb_color.setEnabled(false);
            this.sb_intensity.setEnabled(false);
        }
        this.aSwitch_onoff.setChecked(deviceOperationPO.getValue1() == 0 ? false : true);

    }

    public void refreshGroupUI(){

        groupOperationPO = GroupOperationPO.getGroupOperationPO(relativeId);
        if (groupOperationPO == null){
            groupOperationPO = GroupOperationPO.createOperationPO(relativeId);
        }
        this.tv_color.setText(CommonUtils.getCCTWColor(groupOperationPO.getValue2()));
        this.tv_intensity.setText(groupOperationPO.getValue3()+"%");
        this.sb_color.setProgress(groupOperationPO.getValue2());
        this.sb_intensity.setProgress(groupOperationPO.getValue3());
        if (groupOperationPO.getValue1()==0){
            this.sb_color.setEnabled(false);
            this.sb_intensity.setEnabled(false);
        }
        this.aSwitch_onoff.setChecked(groupOperationPO.getValue1() == 0 ? false : true);
    }
}
