package com.beesmart.blemesh.dao.po;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.google.gson.annotations.Expose;

/**
 * Created by alphawong on 2016/3/17.
 * 情景与组。设备关系描述
 */
@Table(
        name="SceneAffiliationTable"
)
public class SceneAffiliationPO extends Model {
    @Expose
    @Column(
            name = "sceneId"
    )
    private int sceneId;

    @Expose
    @Column(
            name = "relativeId"/*,
            uniqueGroups = {"location1"},
            onUniqueConflicts = {Column.ConflictAction.REPLACE}*/
    )
    private int relativeId;//groupId or deviceId
    @Expose
    @Column(
            name = "relativeType"
    )
    private int relativeType;//0代表device，1代表group

    public int getSceneId() {
        return sceneId;
    }

    public void setSceneId(int sceneId) {
        this.sceneId = sceneId;
    }

    public int getRelativeId() {
        return relativeId;
    }

    public void setRelativeId(int relativeId) {
        this.relativeId = relativeId;
    }

    public int getRelativeType() {
        return relativeType;
    }

    public void setRelativeType(int relativeType) {
        this.relativeType = relativeType;
    }
}
