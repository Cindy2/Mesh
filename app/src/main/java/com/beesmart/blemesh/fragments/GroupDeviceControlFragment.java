package com.beesmart.blemesh.fragments;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.meshlib.operation.OperationInMesh;
import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.log.CSLog;
import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.GroupInfoPO;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.activities.MainActivity;
import com.beesmart.blemesh.adapter.GroupDeviceControlAdapter;
import com.beesmart.blemesh.callback.OnDeviceItemClickListener;
import com.beesmart.blemesh.customwidgets.RecycleViewDivider;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.LocationInfoPO;
import com.beesmart.blemesh.dao.po.SceneDeviceOperationPO;
import com.beesmart.blemesh.utils.DeviceStatusObserver;
import com.beesmart.blemesh.utils.PreferenceUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class GroupDeviceControlFragment extends DialogFragment {
    public static final String ARG_PARAM = "groupId";

    public GroupDeviceControlFragment() {
        // Required empty public constructor
    }
    private int groupId;

    private RecyclerView recyclerView;

    List<DeviceInfoPO> deviceInfoPOs;
    private DeviceInfoPO mDeviceInfoPO;
    private GroupDeviceControlAdapter adapter;
    private int locationId;
    private String locationName;
    private OperationInMesh mOperationInMesh;

    private Dialog mDialog;
    View editView;
    EditText editText;
//    LocationAffiliationPO mLocationAffiliationPO;

    List<Integer> selectedGroups;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group_device_control, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        groupId = getArguments().getInt(ARG_PARAM);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = getActivity().getWindowManager();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        Window window = getDialog().getWindow();
        WindowManager.LayoutParams lp = getDialog().getWindow().getAttributes();
        window.setGravity(Gravity.CENTER);
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.width = metrics.widthPixels;

        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//        setStyle();

        window.setAttributes(lp);
        LocationAffiliationPO affiliationPO = LocationAffiliationPO.getGroupLocationAffiliationPO(groupId);
        locationId = affiliationPO.getLocationId();
        locationName =affiliationPO.getLocationName();
        GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(groupId);
        getDialog().setTitle(groupInfoPO.getGroupName());
        deviceInfoPOs = groupInfoPO.getDeviceListAffiliationsExceptSwitch();
        recyclerView = (RecyclerView) getView();
        recyclerView.addItemDecoration(new RecycleViewDivider(getActivity(), LinearLayout.HORIZONTAL));
        //若不是master手机，不可以进行分组操作
        if (PreferenceUtils.getPrefInt(getActivity(), Constants.NETWORK_FLAG, Constants.NETWORK_FLAG_NONE) == Constants.NETWORK_FLAG_JOIN){
            itemClickListener = null;
        }
        adapter = new GroupDeviceControlAdapter(getActivity(), deviceInfoPOs, itemClickListener);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDetach() {
        // 取消这个fragment的状态订阅，移除订阅者
        DeviceStatusObserver.unsubcribeGroupDevice();
        super.onDetach();
    }
    OnDeviceItemClickListener itemClickListener = new OnDeviceItemClickListener() {
        @Override
        public void onListFragmentInteraction(Object item) {

        }

        @Override
        public void onListFragmentInteraction(final int position) {
            Log.e("onListFragmentInter", "position:::" + position);
            if (locationId == LocationInfoPO.DEFAULT_LOCATION){
                return;
            }
            mDeviceInfoPO = deviceInfoPOs.get(position);

            AlertDialog.Builder operationDialog = new AlertDialog.Builder(getActivity());
            operationDialog.setCancelable(true);
            operationDialog.setItems(R.array.group_device_operate_menu, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //
                    switch (which){
                        case 0://rename
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.rename);
                            editView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_addgroup,null); //编辑对话框
                            editText = (EditText) editView.findViewById(R.id.edittext_groupname);
                            editText.setText(mDeviceInfoPO.getDeviceName());
                            builder.setView(editView);
                            builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    String name = editText.getText().toString();
                                    if (!name.isEmpty()){

                                        mDeviceInfoPO.setDeviceName(name);
                                        mDeviceInfoPO.save();
                                        adapter.notifyItemChanged(position);//通知Adapter刷新item UI显示
                                    }else{
                                        //if empty name，donothing
                                    }
                                }

                            });
                            builder.setNegativeButton(R.string.Cancel,null);
                            builder.create().show();
                            break;
                        case 1://grouping
                            //弹出选择分组对话框
//                            if (mLocationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE){

                                createSetProvisionGroupDialog();
//                            }else{
//                                //TODO 弹出这个组的设备列表，编辑分组
//                            }
                            //加入某组或者新建分组
                            //发送change group command
                            //callback success后在locationAffiliation中删除这个relativeId


                            break;
                        case 2://delete
//                            boolean activeDevice = ((MainActivity) getActivity()).getMeshService().isActiveDevice(mDeviceInfoPO.getNodeId());
//                            if (activeDevice){
                                final View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_delete_device,null);

                                new AlertDialog.Builder(getActivity()).setTitle("Tips").setView(view)
                                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                CheckBox checkedTextView = (CheckBox) view.findViewById(R.id.tv_delete_in_mesh);
                                                if(checkedTextView.isChecked()){
                                                    //reset device
                                                    deleteDeviceInMesh();
                                                }else{
                                                    //just delete from group
                                                    selectedGroups = new ArrayList<Integer>(1);
                                                    selectedGroups.add(0);
                                                    changeGroupInMesh();
                                                }
                                            }
                                        })
                                        .setNegativeButton(R.string.Cancel,null).create().show();

//                            }else{
//                                Toast.makeText(getActivity(), "Operate failed ! Device is not online.", Toast.LENGTH_SHORT).show();
//                            }

                            break;
                    }

                }
            });
            if (mDialog !=null) mDialog.dismiss();
            mDialog =  operationDialog.create();
            mDialog.show();
        }
    };
    /**
     * 在mesh中更改 device groupIds
     */
    private void changeGroupInMesh() {

        int j = 0;
        int[] groupsId = new int[selectedGroups.size()];
        for (int i = 0; i < selectedGroups.size(); i++) {
//                GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(selectedGroups.get(i));
            groupsId[j] = selectedGroups.get(i);
            j++;
        }
        if (mOperationInMesh == null) {
            mOperationInMesh = new OperationInMesh(getActivity().getApplicationContext(), ((MainActivity)getActivity()).getMeshService());
        }

        createProgressDialog();

        mOperationInMesh.editGroups(mDeviceInfoPO.getNodeId(), groupsId,
                MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST,
                new ICommandResponseCallback() {
                    @Override
                    public void onTimeOut() {
                        CSLog.i(LocationDeviceControlFragment.class, "editGroups onTimeOut");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dimissProgressDialog();
                                Toast.makeText(getActivity(), R.string.edit_group_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onSuccess() {
                        CSLog.i(LocationDeviceControlFragment.class, "editGroups onSuccess");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dimissProgressDialog();
                                Toast.makeText(getActivity(), R.string.edit_group_success, Toast.LENGTH_SHORT).show();

                                LocationAffiliationPO locationAffiliationPO = new LocationAffiliationPO();
                                locationAffiliationPO.setRelativeName(mDeviceInfoPO.getDeviceName());
                                locationAffiliationPO.setRelativeId(mDeviceInfoPO.getNodeId());
                                locationAffiliationPO.setLocationId(locationId);
                                locationAffiliationPO.setRelativeType(Constants.CONTROL_TYPE_DEVICE);
                                locationAffiliationPO.save();
                                EventBus.getDefault().post(locationAffiliationPO);
                                deviceInfoPOs.remove(mDeviceInfoPO);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void onFailed() {
                        CSLog.i(LocationDeviceControlFragment.class, "editGroups onFailed");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dimissProgressDialog();
                                Toast.makeText(getActivity(), R.string.edit_group_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }
    private ProgressDialog mProgressDialog;

    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage(getActivity().getResources().getString(R.string.wait_response));

        mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });

        mProgressDialog.show();

    }
    private void dimissProgressDialog() {
        mProgressDialog.dismiss();
    }

    private void createSetProvisionGroupDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.setup_groups);
        builder.setCancelable(false);


        builder.setNeutralButton(R.string.add_new_group, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createAddGroupDialog();
            }
        });
        final List<GroupInfoPO> groupInfoPOs = LocationAffiliationPO.getGroups(locationId);

//
//        final List<GroupInfoPO> groupInfoPOs = GroupInfoPO.getGroupList();
        selectedGroups = new ArrayList<Integer>(1) ;
        //默认选中原先的组
        selectedGroups.add(groupId);

        String[] groupNames = new String[groupInfoPOs.size()];
        int defaultGroupId = -1;
        //在列表中勾选原先的组
        for (int i = 0;i<groupInfoPOs.size();i++) {
            groupNames[i] = groupInfoPOs.get(i).getGroupName();
           if(groupInfoPOs.get(i).getGroupId() == groupId){
               defaultGroupId = i;
           }
        }
        builder.setSingleChoiceItems(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, groupNames),
                defaultGroupId, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedGroups.clear();
                        selectedGroups.add(groupInfoPOs.get(which).getGroupId());
                    }
                });


        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });

        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                //不分组

            }
        });

        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
        ((AlertDialog)mDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedGroups.isEmpty()){
                    Snackbar.make(v,"please select a group!",Snackbar.LENGTH_SHORT).show();
                }else{
                    if (selectedGroups.get(0)!=groupId){//判断组是否改变了
                        changeGroupInMesh();
                    }
                    mDialog.dismiss();
                }
            }
        });
    }

    private void deleteDeviceInMesh() {
        if (mOperationInMesh == null) {
            mOperationInMesh = new OperationInMesh(getActivity().getApplicationContext(), ((MainActivity)getActivity()).getMeshService());
        }

        createProgressDialog();

        mOperationInMesh.deleteDevice(mDeviceInfoPO.getNodeId(),
                MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST,
                new ICommandResponseCallback() {
                    @Override
                    public void onTimeOut() {
                        CSLog.i(LocationDeviceControlFragment.class, "deleteDevice onTimeOut");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dimissProgressDialog();
                                Toast.makeText(getActivity(), R.string.delete_device_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onSuccess() {
                        CSLog.i(LocationDeviceControlFragment.class, "deleteDevice onSuccess");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dimissProgressDialog();
                                Toast.makeText(getActivity(), R.string.delete_device_success, Toast.LENGTH_SHORT).show();
                                //删除数据库中设备相关信息
                                mDeviceInfoPO.remove();
                                SceneDeviceOperationPO.deleteDeviceData(mDeviceInfoPO.getNodeId());
//                                mLocationAffiliationPO.delete();
                                deviceInfoPOs.remove(mDeviceInfoPO);
                                adapter.notifyDataSetChanged();
                                //跟新UI
                                EventBus.getDefault().post(new LocationAffiliationPO());
                            }
                        });
                    };

                    @Override
                    public void onFailed() {
                        CSLog.i(LocationDeviceControlFragment.class, "deleteDevice onFailed");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dimissProgressDialog();
                                Toast.makeText(getActivity(), R.string.delete_device_failed, Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
    }
    private void createAddGroupDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.new_group);
        builder.setCancelable(false);

        final LinearLayout addgroupLayout = (LinearLayout)LayoutInflater.from(getActivity()).inflate(R.layout.dialog_addgroup, null,
                false);
        builder.setView(addgroupLayout);

        EditText editText = (EditText) addgroupLayout.findViewById(R.id.edittext_groupname);

        int groupcount = PreferenceUtils.getPrefInt(getActivity(), Constants.GROUP_NAME_COUNT, 0);
        groupcount++;

//        editText.setHint("group(" + groupcount + ")");

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
//            if (groupname.equals(Constants.EMPTY)) {
//                groupname = editText.getHint().toString();
//            }

            boolean alreadyinGroup = GroupInfoPO.isGroupNameExist(groupname);

            if (groupname.equals(Constants.EMPTY)) {
                Toast.makeText(getActivity(), R.string.group_name_not_empty, Toast.LENGTH_SHORT).show();
                return;
            } else if (alreadyinGroup) {
                Toast.makeText(getActivity(), R.string.group_name_exits, Toast.LENGTH_SHORT).show();
            } else if (groupname.equals(getActivity().getResources().getString(R.string.label_ALL))
                    || groupname.equals(getActivity().getResources().getString(R.string.label_sensor))) {
                Toast.makeText(getActivity(), R.string.group_name_exits, Toast.LENGTH_SHORT).show();
            } else {

                if (groupname.contains("group(")) {
                    int groupcount = PreferenceUtils.getPrefInt(getActivity(), Constants.GROUP_NAME_COUNT, 0);
                    groupcount++;
                    PreferenceUtils.setPrefInt(getActivity(), Constants.GROUP_NAME_COUNT,groupcount);
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
                    locationAffiliationPO.setLocationId(locationId);
                    locationAffiliationPO.setLocationName(locationName);
                    locationAffiliationPO.save();
                } else {
                    Toast.makeText(getActivity(), R.string.group_full, Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
                //暂时不考虑开关的情况，只做CCTW灯
//                if (DeviceInfoPO.getDeviceInfoPO(mLocationAffiliationPO.getRelativeId()).getDeviceType() == MeshConstants.DEVICE_TYPE_SWITCH) {
//                    createSwitchControlGroupDialog();
//                } else {
                createSetProvisionGroupDialog();
//                }
            }
        }

        private void createSwitchControlGroupDialog() {
            //TODO
        }
    }
}
