package com.beesmart.blemesh.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.activities.MainActivity;
import com.beesmart.blemesh.adapter.LocationDeviceControlAdapter;
import com.beesmart.blemesh.callback.OnDeviceItemClickListener;
import com.beesmart.blemesh.customwidgets.RecycleViewDivider;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.LocationInfoPO;
import com.beesmart.blemesh.dao.po.SceneDeviceOperationPO;
import com.beesmart.blemesh.utils.DeviceStatusObserver;
import com.beesmart.blemesh.utils.PreferenceUtils;

/**
 *设备控制的Fragment
 * 显示对应Location的设备列表
 */
public class LocationDeviceControlFragment extends Fragment {

    private RecyclerView recyclerView =null;
    LocationDeviceControlAdapter adapter =null;

    //  Customize parameter argument names
    private static final String LOCATION_ID = "locationId";

    private OnDeviceItemClickListener mListener;

    private int locationId;
    private OperationInMesh mOperationInMesh;

    public LocationDeviceControlFragment() {
    }
    private Dialog mDialog;
    View editView;
    EditText editText;
    List<LocationAffiliationPO> allLocationAffiliations;
    LocationAffiliationPO mLocationAffiliationPO;

    List<Integer> selectedGroups;

    private OnDeviceItemClickListener itemClickOperation = new OnDeviceItemClickListener() {
        @Override
        public void onListFragmentInteraction(final Object item) {

        }

        @Override
        public void onListFragmentInteraction(final int position) {
            mLocationAffiliationPO = allLocationAffiliations.get(position);
            if (locationId == LocationInfoPO.DEFAULT_LOCATION){
                alertLocationAllDialog(position);
            }else {
                alertNormalLocationDialog(position);
            }
        }

    };

    /**
     * 弹出正常的Location Dialog
     * @param position
     */
    private void alertNormalLocationDialog(final int position) {
        AlertDialog.Builder operationDialog = new AlertDialog.Builder(getActivity());
        operationDialog.setCancelable(true);
        operationDialog.setItems(R.array.device_operate_menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
                switch (which) {
                    case 0://rename
                        alertRenameDialog(position);
                        break;
                    case 1://grouping
                        //弹出选择分组对话框
                        if (mLocationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE) {

                            createSetProvisionGroupDialog();
                        } else {
//                                itemClickOperation.onListFragmentInteraction(position);
                            // 弹出这个组的设备列表，编辑分组
                            DialogFragment dialogFragment = new GroupDeviceControlFragment();

                            Bundle args = new Bundle();
                            args.putInt(GroupDeviceControlFragment.ARG_PARAM, mLocationAffiliationPO.getRelativeId());
                            dialogFragment.setArguments(args);
                            dialogFragment.show(getChildFragmentManager(), "groupDeviceControl");
                        }
                        //加入某组或者新建分组
                        //发送change group command
                        //callback success后在locationAffiliation中删除这个relativeId

                        break;
                    case 2://change location
                        alertSelectLocationDialog();
                        break;
                    case 3://delete
                        alertDeleteDialog();
                        break;
                }

            }
        });
        if (mDialog !=null) mDialog.dismiss();
        mDialog =  operationDialog.create();
        mDialog.show();
    }

    /**
     * 删除设备/组
     */
    private void alertDeleteDialog() {
        if (mLocationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_GROUP){
            GroupInfoPO groupInfoPO1 = GroupInfoPO.getGroupInfoPO(mLocationAffiliationPO.getRelativeId());
            if( !groupInfoPO1.getDeviceListAffiliationsExceptSwitch().isEmpty()){
               Toast.makeText(getActivity(), "Please delete all device in group first!", Toast.LENGTH_SHORT).show();
           }else{
                groupInfoPO1.remove();
                mLocationAffiliationPO.delete();
                allLocationAffiliations.remove(mLocationAffiliationPO);
                adapter.notifyDataSetChanged();
           }
        }else {
            boolean activeDevice = ((MainActivity) getActivity()).getMeshService().isActiveDevice(mLocationAffiliationPO.getRelativeId());
            if (activeDevice) {
                //reset device
                new AlertDialog.Builder(getActivity()).setTitle("Warning").setMessage("This operation will remove the device from mesh network and reset it, are you sure to do that?")
                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteDeviceInMesh();
                            }
                        }).setNegativeButton(R.string.Cancel,null).create().show();

            } else {
                Toast.makeText(getActivity(), "Operate failed ! Device is not online.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 重命名设备/组
     * @param position
     */
    private void alertRenameDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.rename);
        editView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_addgroup,null); //编辑对话框
        editText = (EditText) editView.findViewById(R.id.edittext_groupname);
        builder.setView(editView);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String name = editText.getText().toString();
                if (!name.isEmpty()){
                    if (mLocationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE){//devices
                        DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(mLocationAffiliationPO.getRelativeId());

                        deviceInfoPO.setDeviceName(name);
                        deviceInfoPO.save();

                    }else {//group
                        GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(mLocationAffiliationPO.getRelativeId());
                        groupInfoPO.setGroupName(name);
                        groupInfoPO.save();
                    }
                    mLocationAffiliationPO.setRelativeName(name);
                    mLocationAffiliationPO.save();
                    adapter.notifyItemChanged(position);//通知Adapter刷新item UI显示
                }else{
                    //if empty name，donothing
                }
            }

        });
        builder.setNegativeButton(R.string.Cancel,null);
        builder.create().show();
    }

    /**
     * 当在All这个Location时弹出这个Dialog。
     * 以为在All里面只能修改名称和更改Location
     */
    private void alertLocationAllDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.device_operate_menu_in_location_all, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0://Rename
                        alertRenameDialog(position);
                        break;
                    case 1://Change Location
                        alertSelectLocationDialog();
                        break;
                    case 2://Delete
                        alertDeleteDialog();
                        break;
                }
            }
        });
        if (mDialog != null)mDialog.dismiss();
        mDialog = builder.create();
        mDialog.show();
    }

    /**
     * 选择Location对话框
     */
    private void alertSelectLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Change location");
        final List<LocationInfoPO> locationInfoPOs = LocationInfoPO.getLocationInfoPos(false);
        final List<Integer> selectLocation = new ArrayList<>(1);
        selectLocation.add(locationId);
        String[] locationNames = new String[locationInfoPOs.size()];
        int defaultLocationIndex = -1;
        for (int i = 0; i < locationInfoPOs.size(); i++) {
            locationNames[i] = locationInfoPOs.get(i).getLocationName();
            if (locationInfoPOs.get(i).getLocationId() == locationId){
                defaultLocationIndex = i;
            }
        }
        builder.setSingleChoiceItems(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, locationNames),
                defaultLocationIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectLocation.clear();
                        selectLocation.add(locationInfoPOs.get(which).getLocationId());
                    }
                });


        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                mDialog.dismiss();
                if (locationId!=selectLocation.get(0)){
                    mLocationAffiliationPO.setLocationId(selectLocation.get(0));
                    mLocationAffiliationPO.setLocationName(LocationInfoPO.getLocationInfoPo(selectLocation.get(0)).getLocationName());
                    mLocationAffiliationPO.save();
                    if (locationId != LocationInfoPO.DEFAULT_LOCATION){
                        allLocationAffiliations.remove(mLocationAffiliationPO);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        if(mDialog!=null){
            mDialog.dismiss();
        }
        mDialog = builder.create();
        mDialog.show();
    }

    /**
     * 从Mesh中删除Device
     */
    private void deleteDeviceInMesh() {
        if (mOperationInMesh == null) {
            mOperationInMesh = new OperationInMesh(getActivity().getApplicationContext(), ((MainActivity)getActivity()).getMeshService());
        }

        createProgressDialog();

        mOperationInMesh.deleteDevice(mLocationAffiliationPO.getRelativeId(),
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
                                mLocationAffiliationPO.delete();
                                SceneDeviceOperationPO.deleteDeviceData(mLocationAffiliationPO.getRelativeId());
                                allLocationAffiliations.remove(mLocationAffiliationPO);
                                DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(mLocationAffiliationPO.getRelativeId());
                                if (deviceInfoPO!=null) {
                                    deviceInfoPO.remove();
                                }
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }

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

    @SuppressWarnings("unused")
    public static LocationDeviceControlFragment newInstance(int locationId) {
        LocationDeviceControlFragment fragment = new LocationDeviceControlFragment();
        Bundle args = new Bundle();
        args.putInt(LOCATION_ID, locationId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            locationId = getArguments().getInt(LOCATION_ID);
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);


        if(view instanceof RecyclerView){
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.addItemDecoration(new RecycleViewDivider(getActivity(),LinearLayout.HORIZONTAL));
            if(locationId == 1){
                allLocationAffiliations = LocationAffiliationPO.getAllLocationAffiliations();
            }else {
                allLocationAffiliations = LocationAffiliationPO.getLocationAffiliations(locationId);
            }
            //若不是master手机，不可以进行分组操作
            if (PreferenceUtils.getPrefInt(getActivity(), Constants.NETWORK_FLAG, Constants.NETWORK_FLAG_NONE) == Constants.NETWORK_FLAG_JOIN){
                itemClickOperation = null;
            }
            adapter = new LocationDeviceControlAdapter(context,allLocationAffiliations,itemClickOperation);
            recyclerView.setAdapter(adapter);

        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnListFragmentInteractionListener) {
//            mListener = (OnListFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnListFragmentInteractionListener");
//        }
    }

    /**
     * 更新控制列表
     * @param param
     */
    @Subscribe
    public void onDeviceListUpdate(LocationAffiliationPO param){
        //update device list
        allLocationAffiliations.clear();
        if(locationId == 1){
            allLocationAffiliations.addAll(LocationAffiliationPO.getAllLocationAffiliations());

//            DeviceStatusObserver.removeAll();
        }else {
            allLocationAffiliations.addAll(LocationAffiliationPO.getLocationAffiliations(locationId));
        }
        adapter.notifyDataSetChanged();
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        DeviceStatusObserver.removeAll();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * 设置设备分组对话框
     */
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
        selectedGroups = new ArrayList<Integer>() ;
        String[] groupNames = new String[groupInfoPOs.size()];
        for (int i = 0;i<groupInfoPOs.size();i++) {
            groupNames[i] = groupInfoPOs.get(i).getGroupName();
        }
        builder.setSingleChoiceItems(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, groupNames),
                -1, new DialogInterface.OnClickListener() {
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
                    changeGroupInMesh();
                    mDialog.dismiss();
                }
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
                    LocationAffiliationPO newGroup = new LocationAffiliationPO();
                    newGroup.setRelativeType(Constants.CONTROL_TYPE_GROUP);
                    newGroup.setRelativeId(groupInfoPO.getGroupId());
                    newGroup.setRelativeName(groupname);
                    newGroup.setLocationId(locationId);
                    newGroup.setLocationName(mLocationAffiliationPO.getLocationName());
                    newGroup.save();
                    allLocationAffiliations.add(newGroup);
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

        mOperationInMesh.editGroups(mLocationAffiliationPO.getRelativeId(), groupsId,
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

                                mLocationAffiliationPO.delete();
                                allLocationAffiliations.remove(mLocationAffiliationPO);
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
}
