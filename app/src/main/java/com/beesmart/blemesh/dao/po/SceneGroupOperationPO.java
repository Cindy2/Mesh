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
 * 场景操作记录保存，用来保存场景操作中组的操作信息。
 */
@Table(name="SceneGroupOperationTable")
public class SceneGroupOperationPO extends Model{
    /**
     * 场景操作的ID
     */
    @Expose
    @Column(
            name = "sceneGroupOperationId"
    )
    private int sceneGroupOperationId;
    @Expose
    @Column(
            name = "groupId"
    )
    private int groupId;
    @Expose
    @Column(
            name = "groupType"
    )
    private int groupType;
    @Expose
    @Column(
            name = "deivcePower"
    )
    private int groupdata1;
    @Expose
    @Column(
            name = "groupdata2"
    )
    private int groupdata2;
    @Expose
    @Column(
            name = "groupdata3"
    )
    private int groupdata3;
    @Expose
    @Column(
            name = "groupdata4"
    )
    private int groupdata4;
    @Expose
    @Column(
            name = "groupdata5"
    )
    private int groupdata5;
    @Expose
    @Column(
            name = "groupdata6"
    )
    private int groupdata6;
    @Expose
    @Column(
            name = "groupdata7"
    )
    private int groupdata7;
    @Expose
    @Column(
            name = "groupdata8"
    )
    private int groupdata8;
    @Expose
    @Column(
            name = "sceneId"
    )
    private int sceneId;

    public int getGroupType() {
        return groupType;
    }

    public void setGroupType(int groupType) {
        this.groupType = groupType;
    }

    public int getGroupdata1() {
        return groupdata1;
    }

    public void setGroupdata1(int groupdata1) {
        this.groupdata1 = groupdata1;
    }

    public int getGroupdata2() {
        return groupdata2;
    }

    public void setGroupdata2(int groupdata2) {
        this.groupdata2 = groupdata2;
    }

    public int getGroupdata3() {
        return groupdata3;
    }

    public void setGroupdata3(int groupdata3) {
        this.groupdata3 = groupdata3;
    }

    public int getGroupdata4() {
        return groupdata4;
    }

    public void setGroupdata4(int groupdata4) {
        this.groupdata4 = groupdata4;
    }

    public int getGroupdata5() {
        return groupdata5;
    }

    public void setGroupdata5(int groupdata5) {
        this.groupdata5 = groupdata5;
    }

    public int getGroupdata6() {
        return groupdata6;
    }

    public void setGroupdata6(int groupdata6) {
        this.groupdata6 = groupdata6;
    }

    public int getGroupdata7() {
        return groupdata7;
    }

    public void setGroupdata7(int groupdata7) {
        this.groupdata7 = groupdata7;
    }

    public int getGroupdata8() {
        return groupdata8;
    }

    public void setGroupdata8(int groupdata8) {
        this.groupdata8 = groupdata8;
    }
    public int getSceneId() {
        return sceneId;
    }

    public void setSceneId(int sceneId) {
        this.sceneId = sceneId;
    }
    public static SceneGroupOperationPO createSceneGroupOperationPO(){

//        int count = new Select().from(SceneGroupOperationPO.class).count();
        SceneGroupOperationPO po = new Select().from(SceneGroupOperationPO.class).orderBy("id desc").limit(1).executeSingle();
        SceneGroupOperationPO sceneGroupOperationPO = new SceneGroupOperationPO();
        if (po == null){
            sceneGroupOperationPO.setSceneGroupOperationId(1);
        }else
            sceneGroupOperationPO.setSceneGroupOperationId(po.sceneGroupOperationId+1);
        return sceneGroupOperationPO;
    }
    private SceneGroupOperationPO(){}

    private SceneGroupOperationPO(int groupId, int groupType) {
        this.groupId = groupId;
        this.groupType = groupType;
    }

    public void remove() {
        (new Delete()).from(SceneGroupOperationPO.class).where("sceneGroupOperationId = ?", new Object[]{Integer.valueOf(this.sceneGroupOperationId)}).execute();
    }

    public static SceneGroupOperationPO createSceneGroupOperationPO(int groupId, int groupType) {

        SceneGroupOperationPO groupOperationPO = createSceneGroupOperationPO();
        groupOperationPO.setGroupId(groupId);
        groupOperationPO.setGroupType(groupType);
        return groupOperationPO;
    }

//    public static SceneGroupOperationPO getGroupOperationPO(int groupId) {
//        return (SceneGroupOperationPO)(new Select()).from(SceneGroupOperationPO.class).where("groupId = ?", new Object[]{Integer.valueOf(groupId)}).executeSingle();
//    }

    /**
     * 获取情景操作数据
     * @param sceneGroupOperationId
     * @return
     */
    public static SceneGroupOperationPO getGroupOperationPO(int sceneGroupOperationId){
        return (new Select()).from(SceneGroupOperationPO.class).where("sceneGroupOperationId = ?",sceneGroupOperationId).executeSingle();
    }
    /**
     * 获取情景操作数据
     * @param sceneId
     * @param groupId
     * @return
     */
    public static SceneGroupOperationPO getGroupOperationPO(int sceneId,int groupId){
        return (new Select()).from(SceneGroupOperationPO.class).where("sceneId = ? and groupId =?",new Object[]{sceneId,groupId}).executeSingle();
    }
    /**
     * 获取对应情景的所有group数据
     * @param sceneId
     * @return
     */
    public static List<SceneGroupOperationPO> getGroupOperationPOs(int sceneId){
        return (new Select()).from(SceneGroupOperationPO.class).where("sceneId = ?",sceneId).execute();
    }

    /**
     * delete group data relate to scene
     * @param sceneId
     */
    public static void deleteScene(int sceneId) {
        new Delete().from(SceneGroupOperationPO.class).where("sceneId = ?",sceneId).execute();
    }

    public static void deleteGroupData(int groupId){
        new Delete().from(SceneGroupOperationPO.class).where("groupId =?",groupId).execute();
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setSceneGroupOperationId(int sceneGroupOperationId) {
        this.sceneGroupOperationId = sceneGroupOperationId;
    }
}
