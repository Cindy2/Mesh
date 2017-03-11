package com.beesmart.blemesh.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;

import com.nxp.ble.meshlib.CCTWUtil;
import com.nxp.ble.meshlib.callback.ICommandResponseCallback;
import com.nxp.ble.others.MeshConstants;
import com.nxp.utils.po.DeviceOperationPO;
import com.nxp.utils.po.GroupOperationPO;

import java.util.List;

import com.beesmart.blemesh.R;
import com.beesmart.blemesh.activities.MainActivity;
import com.beesmart.blemesh.activities.NewSceneActivity;
import com.beesmart.blemesh.activities.SelectSceneDevicesActivity;
import com.beesmart.blemesh.adapter.SceneAdapter;
import com.beesmart.blemesh.dao.po.SceneDeviceOperationPO;
import com.beesmart.blemesh.dao.po.SceneGroupOperationPO;
import com.beesmart.blemesh.dao.po.SceneInfoPO;


public class SceneFragment extends Fragment {

//    private RecyclerView rcv_scene;
    private FloatingActionButton fabAddScene;
    private CCTWUtil cctwUtil;

    private GridView gv_scene;
    private static final int REQUSET_CODE = 0X101;
    private SceneAdapter adapter;
    public SceneFragment() {
    }

    public static SceneFragment newInstance(String param1, String param2) {
        SceneFragment fragment = new SceneFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scene, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        rcv_scene = (RecyclerView) getView().findViewById(R.id.rcv_scene);
        gv_scene = (GridView) getView().findViewById(R.id.gv_scene);
        fabAddScene = (FloatingActionButton) getView().findViewById(R.id.fab_add_scene);
        fabAddScene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //添加Scene
                startActivityForResult(new Intent(getActivity(), SelectSceneDevicesActivity.class),REQUSET_CODE);
            }
        });

//        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(),2);
//        layoutManager.setOrientation(GridLayoutManager.VERTICAL);
        cctwUtil = new CCTWUtil(getActivity(),((MainActivity)getActivity()).getMeshService());
//        rcv_scene.setLayoutManager(layoutManager);
//        rcv_scene.addItemDecoration(new RecycleViewDivider(getActivity(),8,android.R.color.white));
//        rcv_scene.setAdapter(new SceneRecyclerViewAdapter(getActivity(),SceneInfoPO.getAllSceneInfoPOs(), itemClickCallback));
        adapter = new SceneAdapter(getActivity(),SceneInfoPO.getAllSceneInfoPOs());
        gv_scene.setAdapter(adapter);
        gv_scene.setChoiceMode(GridView.CHOICE_MODE_SINGLE);
        gv_scene.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                createProgressDialog();
                SceneInfoPO sceneInfoPO = (SceneInfoPO) parent.getItemAtPosition(position);
                itemClickCallback.onListFragmentInteraction(sceneInfoPO);
                adapter.selection = sceneInfoPO.getSceneId();
                adapter.notifyDataSetChanged();
                dimissProgressDialog();
            }
        });
        gv_scene.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final SceneInfoPO item = (SceneInfoPO) parent.getItemAtPosition(position);
                final AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
                builder.setItems(R.array.scene_operate_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
//                                Snackbar.make(holder.mView,"重命名",Snackbar.LENGTH_SHORT).show();
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());

                                View view = View.inflate(getActivity(),R.layout.dialog_addgroup,null);
                                final EditText etName = (EditText) view.findViewById(R.id.edittext_groupname);
                                etName.setText(item.getSceneName());
                                builder1.setTitle(R.string.rename_scene).setView(view);
                                builder1.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String newSceneName = etName.getText().toString();
                                        if(!SceneInfoPO.isSceneNameExists(newSceneName)){

                                            item.setSceneName(newSceneName);
                                            item.save();
                                            adapter.notifyDataSetChanged();
                                        }else{
                                            Snackbar.make(etName,"Failed! This name is exists！",Snackbar.LENGTH_SHORT).show();
                                        }

                                    }
                                });
                                builder1.setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                });
                                builder1.create().show();
                                break;

                            case 1:
//                                Snackbar.make(holder.mView,"编辑情景",Snackbar.LENGTH_SHORT).show();
                                int sceneId = item.getSceneId();

                                Intent intent = new Intent(getActivity(), NewSceneActivity.class);
                                intent.putExtra("sceneId", sceneId).putExtra(NewSceneActivity.TYPE,NewSceneActivity.TYPE_EDIT);
                                startActivity(intent);
                                //TODO
                                break;
                            case 2:
                                item.delete();
                                adapter.remove(item);
                                SceneDeviceOperationPO.deleteScene(item.getSceneId());
                                SceneGroupOperationPO.deleteScene(item.getSceneId());
                                adapter.notifyDataSetChanged();
                                break;
                        }
                    }
                });
                builder.create().show();
                return false;
            }
        });
    }

    ICommandResponseCallback iCommandResponseCallback = new ICommandResponseCallback() {
        @Override
        public void onSuccess() {
            Log.d("ResponseCallback","onSuccess");
        }

        @Override
        public void onFailed() {
            //TODO
            Log.d("Response","oFailed callback");
        }

        @Override
        public void onTimeOut() {
            //TODO
            Log.d("Response","onTimeout callback");

        }
    };

    DeviceControlFragment.OnListFragmentInteractionListener itemClickCallback = new DeviceControlFragment.OnListFragmentInteractionListener() {
        @Override
        public void onListFragmentInteraction(SceneInfoPO item) {

            //excute the Scene on the mesh
            //拿到情景Id
            int sceneId = item.getSceneId();

            //根据情景Id 查询SceneAffiliation ，拿到情景对应的设备

            List<SceneDeviceOperationPO> deviceOperationPOs = SceneDeviceOperationPO.getDeviceOperationPOs(sceneId);
            List<SceneGroupOperationPO> groupOperationPOs = SceneGroupOperationPO.getGroupOperationPOs(sceneId);
            //发送情景数据到Mesh
            for (SceneDeviceOperationPO operationPO: deviceOperationPOs) {
                cctwUtil.changeDeviceParameters(operationPO.getDeviceId(),
                        operationPO.getDevicedata1() == 1 ? true : false,
                        (byte) operationPO.getDevicedata3(), (byte) operationPO.getDevicedata2(),
                        MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST,
                        iCommandResponseCallback);
                //保存场景操作的数据

                try {
                    Thread.sleep(75);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (SceneGroupOperationPO po : groupOperationPOs) {
                cctwUtil.changeGroupParameters(po.getGroupId(),
                        po.getGroupdata1() == 1 ? true : false,
                        (byte) po.getGroupdata3(),(byte) po.getGroupdata2(),
                        MeshConstants.CONTROL_FLAG_REQ_BROADCAST | MeshConstants.CONTROL_FLAG_REQ_NEXT_SLOT | MeshConstants.CONTROL_FLAG_RESP_BROADCAST
                );
                try {
                    Thread.sleep(75);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //保存场景操作的数据
            }

            saveSceneDeviceData(deviceOperationPOs);
            saveSceneGroupData(groupOperationPOs);

        }
    };

    private void saveSceneDeviceData(List<SceneDeviceOperationPO> deviceOperationPOs) {
        for (SceneDeviceOperationPO operationPO: deviceOperationPOs) {
            DeviceOperationPO deviceOperationPO = DeviceOperationPO.getDeviceOperationPO(operationPO.getDeviceId());
            if (deviceOperationPO == null){
                deviceOperationPO = DeviceOperationPO.createOperationPO(operationPO.getDeviceId());
            }
            deviceOperationPO.setValue1(operationPO.getDevicedata1());
            deviceOperationPO.setValue2(operationPO.getDevicedata2());
            deviceOperationPO.setValue3(operationPO.getDevicedata3());
            deviceOperationPO.save();
        }
    }

    private void saveSceneGroupData(List<SceneGroupOperationPO> groupOperationPOs) {
        for (SceneGroupOperationPO po : groupOperationPOs) {
            GroupOperationPO groupOperationPO = GroupOperationPO.getGroupOperationPO(po.getGroupId());
            if (groupOperationPO == null){
                groupOperationPO = GroupOperationPO.createOperationPO(po.getGroupId());
            }
            groupOperationPO.setValue1(po.getGroupdata1());
            groupOperationPO.setValue2(po.getGroupdata2());
            groupOperationPO.setValue3(po.getGroupdata3());
            groupOperationPO.save();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUSET_CODE){
            //refresh scene UI
//            rcv_scene.setAdapter(new SceneRecyclerViewAdapter(getActivity(),SceneInfoPO.getAllSceneInfoPOs(), itemClickCallback));
            adapter.clear();
            List<SceneInfoPO> allSceneInfoPOs = SceneInfoPO.getAllSceneInfoPOs();
            adapter.addAll(allSceneInfoPOs);
            adapter.notifyDataSetChanged();
        }
    }
    ProgressDialog mProgressDialog;
    private void createProgressDialog() {
        mProgressDialog = new ProgressDialog(getActivity());

        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage("Scene applying ...");

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
