package com.beesmart.blemesh.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;

import com.nxp.utils.po.DeviceInfoPO;
import com.nxp.utils.po.GroupInfoPO;

import com.beesmart.blemesh.Constants;
import com.beesmart.blemesh.R;
import com.beesmart.blemesh.adapter.ExpandableListAdapter;
import com.beesmart.blemesh.dao.po.LocationAffiliationPO;
import com.beesmart.blemesh.dao.po.SceneInfoPO;

/**
 *设备控制的Fragment
 * 显示对应Location的设备列表
 */
public class DeviceControlFragment extends Fragment {

    private ExpandableListView expandableListView =null;
    ExpandableListAdapter adapter =null;

    // TODO: Customize parameter argument names
    private static final String LOCATION_ID = "locationId";

    private OnListFragmentInteractionListener mListener;

    private int locationId;
    public DeviceControlFragment() {
    }
    private int mGroupChoice;
    private int mChildChoice;

    View editView;
    EditText editText;
    @SuppressWarnings("unused")
    public static DeviceControlFragment newInstance(int columnCount) {
        DeviceControlFragment fragment = new DeviceControlFragment();
        Bundle args = new Bundle();
        args.putInt(LOCATION_ID, columnCount);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            locationId = getArguments().getInt(LOCATION_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);


        if(view instanceof ExpandableListView){
            Context context = view.getContext();
            expandableListView = (ExpandableListView) view;

            if(locationId == 1){
                adapter = new ExpandableListAdapter(context, LocationAffiliationPO.getAllLocationAffiliations());
                expandableListView.setAdapter(adapter);

            }else {
                adapter = new ExpandableListAdapter(context, LocationAffiliationPO.getLocationAffiliations(locationId));
                expandableListView.setAdapter(adapter);
            }
            //set group logo click expand
            adapter.setOnGroupClickLitener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                   if( expandableListView.isGroupExpanded(groupPosition) ){
                       expandableListView.collapseGroup(groupPosition);
                    }else{
                       expandableListView.expandGroup(groupPosition, true);

                   }
                    return false;
                }
            });
        }

        expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final LocationAffiliationPO locationAffiliationPO = (LocationAffiliationPO)adapter.getGroup(position);
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setCancelable(true);
                alertDialog.setItems(R.array.device_operate_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //
                        switch (which){
                            case 0://rename

                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle(R.string.rename);
                                editView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_addgroup,null); //编辑对话框
                                editText = (EditText) editView.findViewById(R.id.edittext_groupname);
                                builder.setView(editView);
                                builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (locationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE){

                                        }
                                        String name = editText.getText().toString();
                                        if (!name.isEmpty()){
                                            if (locationAffiliationPO.getRelativeType() == Constants.CONTROL_TYPE_DEVICE){//devices
                                                DeviceInfoPO deviceInfoPO = DeviceInfoPO.getDeviceInfoPO(locationAffiliationPO.getRelativeId());

                                                deviceInfoPO.setDeviceName(name);
                                                deviceInfoPO.save();

                                            }else {//group
                                                GroupInfoPO groupInfoPO = GroupInfoPO.getGroupInfoPO(locationAffiliationPO.getRelativeId());
                                                groupInfoPO.setGroupName(name);
                                                groupInfoPO.save();
                                            }
                                            locationAffiliationPO.setRelativeName(name);
                                            locationAffiliationPO.save();
                                            adapter.notifyDataSetChanged();
                                        }else{
                                            //empty name
                                        }
                                    }

                                });
                                builder.setNegativeButton(R.string.Cancel,null);
                                builder.create().show();
                                break;
                            case 1://grouping
//                                新建分组
                                //

                                break;
                            case 2://delete

                                break;
                        }

                    }
                });
                Dialog dialog = alertDialog.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
                return false;
            }
        });
        registerForContextMenu(expandableListView);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(SceneInfoPO item);


    }
    // long click to call context menu
    private static final int MENU_RENAME_DEVICE = 0x120;
    private static final int MENU_DEL_DEVICE_ACTIVE = 0x121;
    private static final int MENU_DEL_DEVICE_NOT_ACTIVE = 0x122;
    private static final int MENU_REMOVE_DEVICE_ACTIVE = 0x123;
    // private static final int MENU_REMOVE_DEVICE_NOT_ACTIVE = 0x124;
    private static final int MENU_EDIT_GROUP_ACTIVE = 0x125;
    // private static final int MENU_EDIT_GROUP_NOT_ACTIVE = 0x126;
    private static final int MENU_DEL_SWITCH_ACTIVE = 0x127;
    private static final int MENU_DEL_SWITCH_NOT_ACTIVE = 0x128;

    private static final int MENU_RENAME_GROUP = 0x111;
    private static final int MENU_DEL_GROUP = 0x112;
    @Override
    public boolean onContextItemSelected(MenuItem mi) {
        ExpandableListView.ExpandableListContextMenuInfo menuInfo = (ExpandableListView.ExpandableListContextMenuInfo) mi
                .getMenuInfo();

        mGroupChoice = ExpandableListView.getPackedPositionGroup(menuInfo.packedPosition);
        mChildChoice = ExpandableListView.getPackedPositionChild(menuInfo.packedPosition);

        switch (mi.getItemId()) {

//            case MENU_RENAME_DEVICE://rename device
//                createRenameChildDialog();
//                break;
//
//            case MENU_DEL_DEVICE_ACTIVE:
//                createDeleteDeviceDialog();
//                break;
//
//            case MENU_REMOVE_DEVICE_ACTIVE:
//                createRemoveDeviceDialog();
//                break;
//
//            case MENU_EDIT_GROUP_ACTIVE:
//                createReSelectGroupsDialog();
//                break;
//
//            case MENU_DEL_DEVICE_NOT_ACTIVE:
//                createDeleteDeviceNotActiveDialog();
//                break;
//
//            case MENU_DEL_SWITCH_ACTIVE:
//                createDeleteSwitchDialog();
//                break;
//
//            case MENU_DEL_SWITCH_NOT_ACTIVE:
//                createDeleteSwitchNotActiveDialog();
//                break;
//
//            case MENU_RENAME_GROUP:
//                createRenameGroupDialog();
//                break;
//
//            case MENU_DEL_GROUP:
//                createDeleteGroupDialog();
//                break;
        }

        return true;
    }
}
