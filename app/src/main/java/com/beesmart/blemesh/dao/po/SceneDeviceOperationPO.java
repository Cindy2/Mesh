package com.beesmart.blemesh.dao.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Created by alphawong on 2016/3/18.
 * 场景操作记录保存，用来保存场景操作中设备的操作信息。
 */
@Table(name="SceneDeviceOperationTable")
public class SceneDeviceOperationPO extends Model{
    /**
     * 场景操作的ID
     */
    @Expose
    @Column(
            name = "sceneDeviceOperationId"
    )
    private int sceneDeviceOperationId;
    @Expose
    @Column(
            name = "deviceId"
    )
    private int deviceId;
    @Expose
    @Column(
            name = "deviceType"
    )
    private int deviceType;
    @Expose
    @Column(
            name = "deivcePower"
    )
    private int devicedata1;
    @Expose
    @Column(
            name = "devicedata2"
    )
    private int devicedata2;
    @Expose
    @Column(
            name = "devicedata3"
    )
    private int devicedata3;
    @Expose
    @Column(
            name = "devicedata4"
    )
    private int devicedata4;
    @Expose
    @Column(
            name = "devicedata5"
    )
    private int devicedata5;
    @Expose
    @Column(
            name = "devicedata6"
    )
    private int devicedata6;
    @Expose
    @Column(
            name = "devicedata7"
    )
    private int devicedata7;
    @Expose
    @Column(
            name = "devicedata8"
    )
    private int devicedata8;
    @Expose
    @Column(
            name = "sceneId"
    )
    private int sceneId;

    public int getSceneDeviceOperationId() {
        return sceneDeviceOperationId;
    }

    public void setSceneDeviceOperationId(int sceneDeviceOperationId) {
        this.sceneDeviceOperationId = sceneDeviceOperationId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getDevicedata1() {
        return devicedata1;
    }

    public void setDevicedata1(int devicedata1) {
        this.devicedata1 = devicedata1;
    }

    public int getDevicedata2() {
        return devicedata2;
    }

    public void setDevicedata2(int devicedata2) {
        this.devicedata2 = devicedata2;
    }

    public int getDevicedata3() {
        return devicedata3;
    }

    public void setDevicedata3(int devicedata3) {
        this.devicedata3 = devicedata3;
    }

    public int getDevicedata4() {
        return devicedata4;
    }

    public void setDevicedata4(int devicedata4) {
        this.devicedata4 = devicedata4;
    }

    public int getDevicedata5() {
        return devicedata5;
    }

    public void setDevicedata5(int devicedata5) {
        this.devicedata5 = devicedata5;
    }

    public int getDevicedata6() {
        return devicedata6;
    }

    public void setDevicedata6(int devicedata6) {
        this.devicedata6 = devicedata6;
    }

    public int getDevicedata7() {
        return devicedata7;
    }

    public void setDevicedata7(int devicedata7) {
        this.devicedata7 = devicedata7;
    }

    public int getDevicedata8() {
        return devicedata8;
    }

    public void setDevicedata8(int devicedata8) {
        this.devicedata8 = devicedata8;
    }
    public int getSceneId() {
        return sceneId;
    }

    public void setSceneId(int sceneId) {
        this.sceneId = sceneId;
    }
    public static SceneDeviceOperationPO createSceneDeviceOperationPO(){

//        int count = new Select().from(SceneDeviceOperationPO.class).count();
        SceneDeviceOperationPO po = new Select().from(SceneDeviceOperationPO.class).orderBy("id desc").limit(1).executeSingle();
        SceneDeviceOperationPO sceneDeviceOperationPO = new SceneDeviceOperationPO();
        if (po == null){
            sceneDeviceOperationPO.setSceneDeviceOperationId(1);
        }else
            sceneDeviceOperationPO.setSceneDeviceOperationId(po.sceneDeviceOperationId+1);
        return sceneDeviceOperationPO;
    }
    private SceneDeviceOperationPO(){}

    private SceneDeviceOperationPO(int deviceId,int deviceType) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
    }

    public void remove() {
        (new Delete()).from(SceneDeviceOperationPO.class).where("sceneDeviceOperationId = ?", new Object[]{Integer.valueOf(this.sceneDeviceOperationId)}).execute();
    }

    public static SceneDeviceOperationPO createSceneDeviceOperationPO(int deviceId,int deviceType) {

        SceneDeviceOperationPO deviceOperationPO = createSceneDeviceOperationPO();
        deviceOperationPO.setDeviceId(deviceId);
        deviceOperationPO.setDeviceType(deviceType);
        return deviceOperationPO;
    }

//    public static SceneDeviceOperationPO getDeviceOperationPO(int deviceId) {
//        return (SceneDeviceOperationPO)(new Select()).from(SceneDeviceOperationPO.class).where("deviceId = ?", new Object[]{Integer.valueOf(deviceId)}).executeSingle();
//    }

    /**
     * 获取情景操作数据
     * @param sceneDeviceOperationId
     * @return
     */
    public static SceneDeviceOperationPO getDeviceOperationPO(int sceneDeviceOperationId){
        return (new Select()).from(SceneDeviceOperationPO.class).where("sceneDeviceOperationId = ?",sceneDeviceOperationId).executeSingle();
    }
    /**
     * 获取情景操作数据
     * @param sceneId
     * @param deviceId
     * @return
     */
    public static SceneDeviceOperationPO getDeviceOperationPO(int sceneId,int deviceId){
        return (new Select()).from(SceneDeviceOperationPO.class).where("sceneId = ? and deviceId = ?",new Object[]{sceneId,deviceId}).executeSingle();
    }
    /**
     * 获取对应情景的所有device数据
     * @param sceneId
     * @return
     */
    public static List<SceneDeviceOperationPO> getDeviceOperationPOs(int sceneId){
        return (new Select()).from(SceneDeviceOperationPO.class).where("sceneId = ?",sceneId).execute();
    }

    /**
     * delete device data relate to scene
     * @param sceneId
     */
    public static void deleteScene(int sceneId) {
        new Delete().from(SceneDeviceOperationPO.class).where("sceneId = ?",sceneId).execute();
    }

    public static void deleteDeviceData(int deviceId){
        new Delete().from(SceneDeviceOperationPO.class).where("deviceId =?",deviceId).execute();
    }
}
